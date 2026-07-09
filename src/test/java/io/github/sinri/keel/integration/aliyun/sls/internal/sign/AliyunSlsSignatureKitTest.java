package io.github.sinri.keel.integration.aliyun.sls.internal.sign;

import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliyunSlsSignatureKitTest {
    @Test
    void contentMd5ShouldBeUppercaseHex() {
        Buffer body = Buffer.buffer("{\"hello\": \"world\"}");

        assertEquals("49DFDD54B01CBCD2D2AB5E9E5EE6B9B9", AliyunSlsSignatureKit.contentMd5(body));
    }

    @Test
    void buildSignatureMessageShouldUseContentMd5Header() {
        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-MD5", "49DFDD54B01CBCD2D2AB5E9E5EE6B9B9");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-bodyrawsize", "0");

        String message = AliyunSlsSignatureKit.buildSignatureMessage(
                "POST",
                "application/json",
                "Tue, 23 Aug 2022 12:12:03 GMT",
                headers,
                "/logstores",
                "offset=1&size=10"
        );

        assertEquals("""
                POST
                49DFDD54B01CBCD2D2AB5E9E5EE6B9B9
                application/json
                Tue, 23 Aug 2022 12:12:03 GMT
                x-log-apiversion:0.6.0
                x-log-bodyrawsize:0
                x-log-signaturemethod:hmac-sha1
                /logstores?offset=1&size=10""", message);
    }

    @Test
    void calculateSignatureShouldUseContentMd5HeaderValue() {
        Map<String, String> headers = new TreeMap<>();
        headers.put("Content-MD5", "49DFDD54B01CBCD2D2AB5E9E5EE6B9B9");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-bodyrawsize", "0");

        String signature = AliyunSlsSignatureKit.calculateSignature(
                "POST",
                Buffer.buffer("this body intentionally does not match Content-MD5"),
                "application/json",
                "Tue, 23 Aug 2022 12:12:03 GMT",
                headers,
                "/logstores",
                "offset=1&size=10",
                "test-secret"
        );

        assertEquals("gXVb+439g2a3lRG1kSJRWITwS1w=", signature);
    }

    @Test
    void buildSignatureMessageShouldLeaveEmptyMd5LineWhenHeaderAbsent() {
        Map<String, String> headers = Map.of(
                "x-log-apiversion", "0.6.0",
                "x-log-signaturemethod", "hmac-sha1"
        );

        String message = AliyunSlsSignatureKit.buildSignatureMessage(
                "GET",
                null,
                "Mon, 09 Nov 2015 06:11:16 GMT",
                headers,
                "/logstores",
                "logstoreName=&offset=0&size=1000"
        );

        assertEquals("""
                GET


                Mon, 09 Nov 2015 06:11:16 GMT
                x-log-apiversion:0.6.0
                x-log-signaturemethod:hmac-sha1
                /logstores?logstoreName=&offset=0&size=1000""", message);
    }
}
