package io.github.sinri.keel.integration.aliyun.sls.reader;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * ACS3-HMAC-SHA256 signature calculator for Aliyun OpenAPI V3.
 * <p>
 * This class implements the signature mechanism according to Aliyun OpenAPI V3 specification.
 *
 * @see <a href="https://help.aliyun.com/zh/sdk/product-overview/v3-request-structure-and-signature">V3版本请求体 &
 *         签名机制</a>
 * @since 5.0.0
 */
@Deprecated(forRemoval = true)
@NullMarked
class Acs3SignatureCalculator {
    private static final String ALGORITHM = "ACS3-HMAC-SHA256";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";

    private final String accessKeySecret;

    public Acs3SignatureCalculator(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    /**
     * Calculate the ACS3-HMAC-SHA256 signature.
     *
     * @param method        HTTP method (e.g., "POST", "GET")
     * @param uri           Request URI path (e.g., "/logstores/test/logs")
     * @param queryString   Query string (can be null or empty)
     * @param headers       All request headers
     * @param signedHeaders Comma-separated list of header names to include in signature
     * @param payload       Request body as string
     * @param timestamp     ISO8601 timestamp (e.g., "2026-01-13T12:34:56Z")
     * @return Base64-encoded signature
     */
    public String calculateSignature(
            String method,
            String uri,
            @Nullable String queryString,
            Map<String, String> headers,
            String signedHeaders,
            String payload,
            String timestamp
    ) {
        // Step 1: Build canonical request
        String canonicalRequest = buildCanonicalRequest(
                method,
                uri,
                queryString,
                headers,
                signedHeaders,
                payload
        );

        // Step 2: Build string to sign
        String stringToSign = buildStringToSign(canonicalRequest, timestamp);

        // Step 3: Calculate signature
        return sign(stringToSign);
    }

    /**
     * Build the canonical request string.
     * <p>
     * Format:
     * <pre>
     * HTTPMethod + "\n" +
     * CanonicalURI + "\n" +
     * CanonicalQueryString + "\n" +
     * CanonicalHeaders + "\n" +
     * SignedHeaders + "\n" +
     * HashedPayload
     * </pre>
     *
     * @param method        HTTP method
     * @param uri           Request URI
     * @param queryString   Query string
     * @param headers       Request headers
     * @param signedHeaders Signed header names (comma-separated)
     * @param payload       Request payload
     * @return Canonical request string
     */
    private String buildCanonicalRequest(
            String method,
            String uri,
            @Nullable String queryString,
            Map<String, String> headers,
            String signedHeaders,
            String payload
    ) {
        StringBuilder canonical = new StringBuilder();

        // 1. HTTP Method
        canonical.append(method.toUpperCase()).append("\n");

        // 2. Canonical URI
        canonical.append(uri).append("\n");

        // 3. Canonical Query String
        if (queryString != null && !queryString.isEmpty()) {
            canonical.append(canonicalizeQueryString(queryString));
        }
        canonical.append("\n");

        // 4. Canonical Headers
        canonical.append(buildCanonicalHeaders(headers, signedHeaders)).append("\n");

        // 5. Signed Headers
        canonical.append(signedHeaders).append("\n");

        // 6. Hashed Payload
        canonical.append(hashSha256(payload));

        return canonical.toString();
    }

    /**
     * Build canonical headers string.
     * <p>
     * Format: header1:value1\nheader2:value2\n...
     *
     * @param headers       All request headers
     * @param signedHeaders Comma-separated list of headers to include
     * @return Canonical headers string
     */
    private String buildCanonicalHeaders(Map<String, String> headers, String signedHeaders) {
        String[] headerNames = signedHeaders.split(",");
        TreeMap<String, String> sortedHeaders = new TreeMap<>();

        for (String headerName : headerNames) {
            String trimmedName = headerName.trim().toLowerCase();
            String headerValue = headers.get(trimmedName);
            if (headerValue != null) {
                // Trim and normalize whitespace in header values
                sortedHeaders.put(trimmedName, headerValue.trim().replaceAll("\\s+", " "));
            }
        }

        return sortedHeaders.entrySet().stream()
                            .map(entry -> entry.getKey() + ":" + entry.getValue())
                            .collect(Collectors.joining("\n")) + "\n";
    }

    /**
     * Canonicalize query string by sorting parameters.
     *
     * @param queryString Raw query string
     * @return Canonicalized query string
     */
    private String canonicalizeQueryString(@Nullable String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return "";
        }

        String[] params = queryString.split("&");
        TreeMap<String, String> sortedParams = new TreeMap<>();

        for (String param : params) {
            int idx = param.indexOf('=');
            if (idx > 0) {
                String key = param.substring(0, idx);
                String value = param.substring(idx + 1);
                sortedParams.put(key, value);
            } else {
                sortedParams.put(param, "");
            }
        }

        return sortedParams.entrySet().stream()
                           .map(entry -> entry.getValue()
                                              .isEmpty() ? entry.getKey() : entry.getKey() + "=" + entry.getValue())
                           .collect(Collectors.joining("&"));
    }

    /**
     * Build the string to sign.
     * <p>
     * Format:
     * <pre>
     * Algorithm + "\n" +
     * HashedCanonicalRequest
     * </pre>
     *
     * @param canonicalRequest Canonical request string
     * @param timestamp        ISO8601 timestamp
     * @return String to sign
     */
    private String buildStringToSign(String canonicalRequest, String timestamp) {
        return ALGORITHM + "\n" + hashSha256(canonicalRequest);
    }

    /**
     * Calculate HMAC-SHA256 signature.
     *
     * @param stringToSign String to sign
     * @return Base64-encoded signature
     */
    private String sign(String stringToSign) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec signingKey = new SecretKeySpec(
                    accessKeySecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(signingKey);
            byte[] signatureBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate signature", e);
        }
    }

    /**
     * Hash a string using SHA-256.
     *
     * @param data Data to hash
     * @return Hex-encoded hash
     */
    private String hashSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256);
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Convert byte array to lowercase hexadecimal string.
     *
     * @param bytes Byte array
     * @return Hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
