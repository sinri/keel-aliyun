package io.github.sinri.keel.integration.aliyun.sls.internal.sign;

import io.github.sinri.keel.core.utils.DigestUtils;
import io.vertx.core.buffer.Buffer;
import org.jspecify.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


public class AliyunSlsSignatureKit {
    /**
     * Calculate SLS LOG signature (HMAC-SHA1).
     * <p>
     * 如果是GET请求这样没有HTTP Request Body，则在签名计算过程里contentType和body均对应作空行处理。     *
     *
     * @param method          HTTP method
     * @param body            HTTP request body (can be null)
     * @param contentType     Content-Type header (can be null)
     * @param date            Request date
     * @param headers         Request headers
     * @param uri             Request URI
     * @param queries         Query parameters (can be null)
     * @param accessKeySecret Access key secret for signing
     * @return Base64-encoded signature
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/common-request-headers">日志服务API的公共请求头信息</a>
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/request-signatures">请求签名</a>
     */
    public static String calculateSignature(
            String method,
            @Nullable Buffer body,
            @Nullable String contentType,
            String date,
            Map<String, String> headers,
            String uri,
            @Nullable String queries,
            String accessKeySecret
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

    /**
     * Get the current date in GMT format as required by SLS API.
     * <p>
     * This method generates a date string in RFC1123 format, which is used in the Date header
     * and signature calculation for Aliyun SLS API requests.
     *
     * @return Date string in RFC1123 format (e.g., "Wed, 14 Jan 2026 12:34:56 GMT")
     */
    public static String getGMTDate() {
        var RFC1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
        SimpleDateFormat sdf = new SimpleDateFormat(RFC1123_PATTERN, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }
}
