package io.github.sinri.keel.integration.aliyun.sls.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SlsMetricRecorderTest {
    @Test
    void buildLabelsContentShouldNotMutateMutableLabels() {
        Map<String, String> labels = new TreeMap<>();
        labels.put("idc", "idc1");
        labels.put("ip", "192.0.2.0");

        String content = SlsMetricRecorder.buildLabelsContent(labels, "recorder-source");

        assertEquals("idc#$#idc1|ip#$#192.0.2.0|source#$#recorder-source", content);
        assertEquals(Map.of("idc", "idc1", "ip", "192.0.2.0"), labels);
    }

    @Test
    void buildLabelsContentShouldAcceptImmutableLabels() {
        Map<String, String> labels = Map.of("idc", "idc1");

        String content = assertDoesNotThrow(() -> SlsMetricRecorder.buildLabelsContent(labels, "recorder-source"));

        assertEquals("idc#$#idc1|source#$#recorder-source", content);
    }

    @Test
    void buildLabelsContentShouldUseRecorderSourceWhenSourceLabelExists() {
        Map<String, String> labels = new TreeMap<>();
        labels.put("idc", "idc1");
        labels.put("source", "caller-source");

        String content = SlsMetricRecorder.buildLabelsContent(labels, "recorder-source");

        assertEquals("idc#$#idc1|source#$#recorder-source", content);
        assertEquals(Map.of("idc", "idc1", "source", "caller-source"), labels);
    }
}
