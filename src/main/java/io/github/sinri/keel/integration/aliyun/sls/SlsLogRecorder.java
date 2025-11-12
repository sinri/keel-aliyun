package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.record.LoggingRecord;
import io.github.sinri.keel.logger.impl.record.QueuedLogRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;

public class SlsLogRecorder extends QueuedLogRecorder {
    private final QueuedLogWriter<LoggingRecord> writer;

    public SlsLogRecorder(@Nonnull String topic, @Nonnull QueuedLogWriter<LoggingRecord> writer) {
        super(topic);
        this.writer = writer;
    }

    @Nonnull
    @Override
    protected QueuedLogWriter<LoggingRecord> writer() {
        return writer;
    }
}
