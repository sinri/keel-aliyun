package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.adapter.Adapter;
import io.github.sinri.keel.logger.api.adapter.LogWriter;
import io.github.sinri.keel.logger.api.adapter.Render;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.record.LogRecord;
import io.github.sinri.keel.logger.impl.issue.AbstractIssueRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SlsIssueRecorder<T extends IssueRecord<T>> extends AbstractIssueRecorder<T, LogRecord> {
    @Nonnull
    private final Adapter<T, LogRecord> adapter;
    @Nonnull
    private final Supplier<T> issueRecordSupplier;

    public SlsIssueRecorder(@Nonnull String topic, @Nonnull Supplier<T> issueRecordSupplier, @Nonnull QueuedLogWriter<LogRecord> writer) {
        super(topic);
        this.adapter = new AdapterImpl<>(writer);
        this.issueRecordSupplier = issueRecordSupplier;
    }

    @Nonnull
    @Override
    public Supplier<T> issueRecordSupplier() {
        return issueRecordSupplier;
    }

    @Nonnull
    @Override
    public Adapter<T, LogRecord> adapter() {
        return adapter;
    }

    private static class AdapterImpl<T extends IssueRecord<T>> implements Adapter<T, LogRecord> {
        private final LogWriter<LogRecord> writer;
        private final Render<T, LogRecord> render;

        public AdapterImpl(@Nonnull QueuedLogWriter<LogRecord> writer) {
            this.writer = writer;
            this.render = new Issue2LogRenderImpl<>();
        }

        @Nonnull
        @Override
        public Render<T, LogRecord> render() {
            return render;
        }

        @Nonnull
        @Override
        public LogWriter<LogRecord> writer() {
            return writer;
        }
    }

}
