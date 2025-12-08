package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.logger.factory.StdoutLoggerFactory;
import io.github.sinri.keel.core.utils.DigestUtils;
import io.github.sinri.keel.core.utils.NetUtils;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.internal.protocol.Lz4Utils;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 阿里云日志服务上传 API 封装类。
 *
 * @since 5.0.0
 */
public class AliyunSLSLogPutter implements Closeable {
    @NotNull
    private final String accessKeyId;
    @NotNull
    private final String accessKeySecret;
    @NotNull
    private final WebClient webClient;
    @NotNull
    private final String endpoint;
    @NotNull
    private final Logger logger;

    public AliyunSLSLogPutter(@NotNull Vertx vertx, @NotNull String accessKeyId, @NotNull String accessKeySecret, @NotNull String endpoint) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.webClient = WebClient.create(vertx);
        this.endpoint = endpoint;
        this.logger = StdoutLoggerFactory.getInstance()
                                         .createLogger(AliyunSLSLogPutter.class.getName());
    }

    /**
     * Build source from configuration.
     * Source Expression should be:
     * - EMPTY/BLANK STRING or NULL: use SLS default source generation;
     * - A TEMPLATED STRING
     * --- Rule 1: Replace [IP] to local address;
     */
    @NotNull
    public static String buildSource(@Nullable String configuredSourceExpression) {
        if (configuredSourceExpression == null || configuredSourceExpression.isBlank()) {
            return "";
        }
        // Rule 1: Replace [IP] to local address
        String localHostAddress = NetUtils.getLocalHostAddress();
        if (localHostAddress == null) {
            StdoutLoggerFactory.getInstance()
                               .createLogger(AliyunSLSLogPutter.class.getName())
                               .warning("Could not get local host address for SLS source!");
            return "";
        }
        return configuredSourceExpression.replaceAll("\\[IP]", localHostAddress);
    }

    @Override
    public void close() {
        logger.debug("Closing AliyunSLSLogPutter web client");
        this.webClient.close();
    }

    @NotNull
    public Future<Void> putLogs(@NotNull String project, @NotNull String logstore, @NotNull LogGroup logGroup) {
        return putLogsImpl(project, logstore, logGroup);
    }

    /**
     * 调用PutLogs API。
     *
     * @param project  Project name
     * @param logstore Logstore name
     * @param logGroup LogGroup to be sent
     * @return Future of void if successful, or failed future with an error message
     */
    @NotNull
    private Future<Void> putLogsImpl(@NotNull String project, @NotNull String logstore, @NotNull LogGroup logGroup) {
        String uri = String.format("/logstores/%s/shards/lb", logstore);
        String url = String.format("https://%s.%s%s", project, endpoint, uri);

        String date = getGMTDate();
        String contentType = "application/x-protobuf";

        Map<String, String> headers = new HashMap<>();
        headers.put("Date", date);
        headers.put("Content-Type", contentType);
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("x-log-compresstype", "lz4");
        headers.put("Host", project + "." + endpoint);

        // Convert LogGroup to protobuf format
        Buffer raw = serializeLogGroup(logGroup);
        headers.put("x-log-bodyrawsize", String.valueOf(raw.length()));
        Buffer payload = Lz4Utils.compress(raw);
        headers.put("Content-Length", String.valueOf(payload.length()));

        try {
            var contentMd5 = Base64.getEncoder().encodeToString(
                    java.security.MessageDigest.getInstance("MD5").digest(payload.getBytes()));
            headers.put("Content-MD5", contentMd5);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }

        // Calculate signature and add authorization header
        String signature = calculateSignature(
                "POST",
                payload,
                contentType,
                date,
                headers,
                uri,
                null);
        headers.put("Authorization", "LOG " + accessKeyId + ":" + signature);

        HttpRequest<Buffer> request = this.webClient.postAbs(url);
        headers.forEach(request::putHeader);
        return request.sendBuffer(payload)
                      .compose(bufferHttpResponse -> {
                          if (bufferHttpResponse.statusCode() != 200) {
                              // System.out.println("write to sls: 200");
                              logger.error("put log failed [" + bufferHttpResponse.statusCode() + "] "
                                      + bufferHttpResponse.bodyAsString());
                          }
                          return Future.succeededFuture();
                      })
                      .recover(throwable -> {
                          logger.error(log -> log.exception(throwable).message("put log failed [X]"));
                          return Future.succeededFuture();
                      })
                      .mapEmpty();
    }

    /**
     * According to Aliyun SLS API documentation, we should send LogGroupList;
     * But let's try sending just the first LogGroup to see if that works.
     */
    @NotNull
    private Buffer serializeLogGroup(@NotNull LogGroup logGroup) {
        return Buffer.buffer(logGroup.toProtobuf().toByteArray());
    }

    /**
     * Get the current date in GMT format as required by SLS API.
     *
     * @return Date string in RFC1123 format
     */
    @NotNull
    private String getGMTDate() {
        var RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
        SimpleDateFormat sdf = new SimpleDateFormat(RFC1123_PATTERN, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    /**
     * 如果是GET请求这样没有HTTP Request Body，则在签名计算过程里contentType和body均对应作空行处理。
     *
     * @param method      HTTP方法，如 GET, POST 等
     * @param body        HTTP请求体，可以为 null
     * @param contentType Content-Type 头部，可以为 null
     * @param date        请求时间
     * @param headers     请求头部集合
     * @param uri         请求URI
     * @param queries     查询参数字符串，可以为 null
     * @return 计算得到的签名字符串
     */
    private String calculateSignature(
            @NotNull String method,
            @Nullable Buffer body,
            @Nullable String contentType,
            @NotNull String date,
            @NotNull Map<String, String> headers,
            @NotNull String uri,
            @Nullable String queries
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append("\n");
        if (body != null) {
            String md5 = DigestUtils.MD5(body.getBytes());
            sb.append(md5).append("\n");
        } else {
            sb.append("\n");
        }
        if (contentType != null) {
            sb.append(contentType).append("\n");
        } else {
            sb.append("\n");
        }
        sb.append(date).append("\n");

        List<String> headerLines = headers.keySet().stream()
                                          .filter(headerName -> headerName.startsWith("x-log-") || headerName.startsWith("x-acs-"))
                                          .sorted().map(x -> x + ":" + headers.get(x))
                                          .toList();
        headerLines.forEach(x -> sb.append(x).append("\n"));

        sb.append(uri);
        if (queries != null && !queries.isBlank()) {
            sb.append("?").append(queries);
        }

        var signStr = sb.toString();

        try {
            // Calculate HMAC-SHA1 signature
            var HmacSHA1 = "HmacSHA1";
            Mac mac = Mac.getInstance(HmacSHA1);
            SecretKeySpec signingKey = new SecretKeySpec(
                    accessKeySecret.getBytes(StandardCharsets.UTF_8),
                    HmacSHA1);
            mac.init(signingKey);
            byte[] signatureBytes = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));

            // Encode signature in Base64
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
