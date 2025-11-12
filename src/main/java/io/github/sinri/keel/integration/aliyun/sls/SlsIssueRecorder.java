package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.adapter.Adapter;
import io.github.sinri.keel.logger.api.adapter.LogWriter;
import io.github.sinri.keel.logger.api.adapter.Render;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.record.LoggingRecord;
import io.github.sinri.keel.logger.impl.issue.AbstractIssueRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class SlsIssueRecorder<T extends IssueRecord<T>> extends AbstractIssueRecorder<T, LoggingRecord> {
    @Nonnull
    private final Adapter<T, LoggingRecord> adapter;
    @Nonnull
    private final Supplier<T> issueRecordSupplier;

    public SlsIssueRecorder(@Nonnull String topic, @Nonnull Supplier<T> issueRecordSupplier, @Nonnull QueuedLogWriter<LoggingRecord> writer) {
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
    public Adapter<T, LoggingRecord> adapter() {
        return adapter;
    }

    private static class AdapterImpl<T extends IssueRecord<T>> implements Adapter<T, LoggingRecord> {
        private final LogWriter<LoggingRecord> writer;
        private final Render<T, LoggingRecord> render;

        public AdapterImpl(@Nonnull QueuedLogWriter<LoggingRecord> writer) {
            this.writer = writer;
            this.render = new Issue2LogRenderImpl<>();
        }

        @Nonnull
        @Override
        public Render<T, LoggingRecord> render() {
            return render;
        }

        @Nonnull
        @Override
        public LogWriter<LoggingRecord> writer() {
            return writer;
        }
    }

}
