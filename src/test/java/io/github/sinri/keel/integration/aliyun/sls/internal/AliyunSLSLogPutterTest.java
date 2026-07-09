package io.github.sinri.keel.integration.aliyun.sls.internal;

import com.google.protobuf.DynamicMessage;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogContent;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogItem;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogTag;
import io.github.sinri.keel.integration.aliyun.sls.internal.protocol.LogEntityDescriptors;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AliyunSLSLogPutterTest {
    @Test
    void serializeLogGroupShouldUseLogGroupBody() throws Exception {
        LogGroup logGroup = new LogGroup("test-topic", "test-source")
                .addLogTag(new LogTag("env", "test"))
                .addLogItem(new LogItem(1_720_000_000)
                        .addContent(new LogContent("message", "hello")));

        Buffer serialized = AliyunSLSLogPutter.serializeLogGroup(logGroup);
        var descriptor = LogEntityDescriptors.getInstance().getLogGroupDescriptor();
        DynamicMessage parsed = DynamicMessage.parseFrom(descriptor, serialized.getBytes());

        assertEquals("test-topic", parsed.getField(descriptor.findFieldByName("Topic")));
        assertEquals("test-source", parsed.getField(descriptor.findFieldByName("Source")));
        assertEquals(1, parsed.getRepeatedFieldCount(descriptor.findFieldByName("Logs")));
        assertEquals(1, parsed.getRepeatedFieldCount(descriptor.findFieldByName("LogTags")));
    }
}
