package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.record.LogRecord;
import io.github.sinri.keel.logger.impl.record.QueuedLogRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;

public class SlsLogRecorder extends QueuedLogRecorder {
    private final QueuedLogWriter<LogRecord> writer;

    public SlsLogRecorder(@Nonnull String topic, @Nonnull QueuedLogWriter<LogRecord> writer) {
        super(topic);
        this.writer = writer;
    }

    @Nonnull
    @Override
    protected QueuedLogWriter<LogRecord> writer() {
        return writer;
    }
}
