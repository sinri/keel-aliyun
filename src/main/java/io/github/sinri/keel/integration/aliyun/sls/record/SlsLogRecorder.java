package io.github.sinri.keel.integration.aliyun.sls.record;

import io.github.sinri.keel.logger.api.record.LogRecord;
import io.github.sinri.keel.logger.impl.record.QueuedLogRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;

public class SlsLogRecorder extends QueuedLogRecorder {
    private final SlsLogWriter writer;

    public SlsLogRecorder(@Nonnull String topic, @Nonnull SlsLogWriter writer) {
        super(topic);
        this.writer = writer;
    }

    @Nonnull
    @Override
    protected QueuedLogWriter<LogRecord> writer() {
        return writer;
    }
}
