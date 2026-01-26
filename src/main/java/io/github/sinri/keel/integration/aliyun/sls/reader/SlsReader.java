package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.annotations.TechnicalPreview;
import io.github.sinri.keel.base.async.Keel;
import io.github.sinri.keel.base.configuration.NotConfiguredException;
import io.github.sinri.keel.base.logger.factory.StdoutLoggerFactory;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.internal.protocol.Lz4Utils;
import io.github.sinri.keel.integration.aliyun.sls.internal.sign.AliyunSlsSignatureKit;
import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.vertx.core.Closeable;
import io.vertx.core.Completable;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.jspecify.annotations.NullMarked;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 本类提供阿里云日志服务（SLS）的日志获取类功能。
 * <p>
 * 关于 SLS 公共接口调用，使用 SLS 专用的 LOG 签名机制（HMAC-SHA1），而非通用的 ACS3 签名。
 *
 * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">GetLogsV2 API</a>
 * @since 5.0.0
 */
@TechnicalPreview(since = "5.0.0")
@NullMarked
public class SlsReader implements Closeable {
    private static final String CONTENT_TYPE = "application/json";
    private static final String ACCEPT_ENCODING = "lz4";

    private final Keel keel;
    private final AliyunSlsConfigElement aliyunSlsConfigElement;
    private final WebClient webClient;
    private final Logger logger;

    public SlsReader(Keel keel, AliyunSlsConfigElement aliyunSlsConfigElement) {
        this.keel = keel;
        this.aliyunSlsConfigElement = aliyunSlsConfigElement;
        this.webClient = WebClient.create(keel);
        this.logger = StdoutLoggerFactory.getInstance()
                                         .createLogger(SlsReader.class.getName());
        this.logger.visibleLevel(LogLevel.WARNING);
    }

    public Keel getKeel() {
        return keel;
    }

    @Override
    public void close(Completable<Void> completion) {
        //logger.debug("Closing SlsReader web client");
        this.webClient.close();
        completion.succeed();
    }

    public Future<Void> close() {
        Promise<Void> promise = Promise.promise();
        close(promise);
        return promise.future();
    }

    /**
     * 查询指定Project下某个Logstore中的原始日志数据，返回结果显示某时间区间中的原始日志（返回结果压缩后传输）。
     *
     * @param request GetLogsV2 request parameters
     * @return Future containing the response as JsonObject
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">GetLogsV2</a>
     */
    public Future<GetLogsV2Response> callGetLogsV2(GetLogsV2Request request) {
        try {
            String project = aliyunSlsConfigElement.getProject();
            String logstore = aliyunSlsConfigElement.getLogstore();
            String endpoint = aliyunSlsConfigElement.getEndpoint();
            String accessKeyId = aliyunSlsConfigElement.getAccessKeyId();
            String accessKeySecret = aliyunSlsConfigElement.getAccessKeySecret();

            return callGetLogsV2Impl(project, logstore, endpoint, accessKeyId, accessKeySecret, request)
                    .compose(jsonObject -> {
                        return Future.succeededFuture(new GetLogsV2Response(jsonObject));
                    });
        } catch (NotConfiguredException e) {
            return Future.failedFuture(e);
        }
    }

    /**
     * Internal implementation of GetLogsV2 API call.
     *
     * @param project         Project name
     * @param logstore        Logstore name
     * @param endpoint        SLS endpoint
     * @param accessKeyId     Access key ID
     * @param accessKeySecret Access key secret
     * @param request         Request parameters
     * @return Future containing the response
     */
    private Future<JsonObject> callGetLogsV2Impl(
            String project,
            String logstore,
            String endpoint,
            String accessKeyId,
            String accessKeySecret,
            GetLogsV2Request request
    ) {
        // Build URI and URL
        String uri = String.format("/logstores/%s/logs", logstore);
        String url = String.format("https://%s.%s%s", project, endpoint, uri);

        // Get current GMT date
        String date = AliyunSlsSignatureKit.getGMTDate();

        // Prepare request body
        String requestBody = request.toJsonExpression();
        Buffer bodyBuffer = Buffer.buffer(requestBody);

        // Build host header
        String host = project + "." + endpoint;

        // Build headers for SLS LOG signature
        Map<String, String> headers = new HashMap<>();
        headers.put("Date", date);
        headers.put("Content-Type", CONTENT_TYPE);
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("Host", host);
        headers.put("Content-Length", String.valueOf(bodyBuffer.length()));
        headers.put("Accept-Encoding", ACCEPT_ENCODING);

        // Calculate Content-MD5
        try {
            var contentMd5 = Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("MD5").digest(bodyBuffer.getBytes()));
            headers.put("Content-MD5", contentMd5);
        } catch (NoSuchAlgorithmException e) {
            return Future.failedFuture(new RuntimeException("MD5 algorithm not available", e));
        }

        // Calculate SLS LOG signature
        String signature = AliyunSlsSignatureKit.calculateSignature(
                "POST",
                bodyBuffer,
                CONTENT_TYPE,
                date,
                headers,
                uri,
                null,
                accessKeySecret
        );

        // Build Authorization header with LOG prefix
        headers.put("Authorization", "LOG " + accessKeyId + ":" + signature);

        // Build and send request
        HttpRequest<Buffer> httpRequest = webClient.postAbs(url);
        headers.forEach(httpRequest::putHeader);

        logger.debug(log -> log.message("Sending GetLogsV2 request")
                               .context("url", url)
                               .context("date", date));

        return httpRequest.sendBuffer(bodyBuffer)
                          .compose(this::handleResponse)
                          .recover(throwable -> {
                              logger.error(log -> log
                                      .exception(throwable)
                                      .message("GetLogsV2 request failed"));
                              return Future.failedFuture(throwable);
                          });
    }

    /**
     * Handle the HTTP response from GetLogsV2 API.
     *
     * @param response HTTP response
     * @return Future containing parsed JsonObject
     */
    private Future<JsonObject> handleResponse(HttpResponse<Buffer> response) {
        int statusCode = response.statusCode();

        if (statusCode != 200) {
            String errorMessage = String.format(
                    "GetLogsV2 failed with status %d: %s",
                    statusCode,
                    response.bodyAsString()
            );
            logger.error(errorMessage);
            return Future.failedFuture(errorMessage);
        }

        try {
            Buffer responseBody = response.bodyAsBuffer();
            if (responseBody == null || responseBody.length() == 0) {
                return Future.succeededFuture(new JsonObject());
            }

            response.headers()
                    .forEach(header -> logger.info(log -> log.message("Response header " + header.getKey() + "=" + header.getValue())));

            // Check if response is compressed
            String compressType = response.getHeader("x-log-compresstype");
            //System.out.println("contentEncoding: "+contentEncoding);
            logger.info("x-log-compresstype: " + compressType);
            Buffer decompressedBody;

            if ("lz4".equalsIgnoreCase(compressType)) {
                // Get original length from header if available
                String rawSizeHeader = response.getHeader("x-log-bodyrawsize");
                if (rawSizeHeader != null) {
                    int originalLength = Integer.parseInt(rawSizeHeader);
                    decompressedBody = Lz4Utils.decompress(responseBody, originalLength);
                } else {
                    // If raw size not provided, try to determine from compressed data
                    logger.warning("Response is LZ4 compressed but x-log-bodyrawsize header is missing");
                    // For now, return error as we need the original size
                    return Future.failedFuture("Cannot decompress LZ4 response without x-log-bodyrawsize header");
                }
            } else {
                // Response is not compressed
                System.out.println("responseBody: " + responseBody);
                decompressedBody = responseBody;
            }

            // Parse JSON
            JsonObject result = decompressedBody.toJsonObject();
            logger.debug(log -> log.message("GetLogsV2 request successful")
                                   .context("resultSize", result.size()));

            return Future.succeededFuture(result);
        } catch (Exception e) {
            logger.error(log -> log.exception(e)
                                   .message("Failed to parse GetLogsV2 response"));
            return Future.failedFuture(e);
        }
    }
}
