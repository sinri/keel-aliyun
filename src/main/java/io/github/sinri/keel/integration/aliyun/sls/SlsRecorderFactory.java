package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.event.EventRecorder;
import io.github.sinri.keel.logger.api.event.RecorderFactory;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.issue.IssueRecorder;
import io.github.sinri.keel.logger.api.record.LoggingRecord;
import io.github.sinri.keel.logger.api.record.LoggingRecorder;
import io.github.sinri.keel.logger.impl.record.QueuedLogWriter;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SlsRecorderFactory implements RecorderFactory<LoggingRecord> {
    private static final SlsRecorderFactory INSTANCE = new SlsRecorderFactory();
    private final QueuedLogWriter<LoggingRecord> writer;

    private SlsRecorderFactory() {
        QueuedLogWriter<LoggingRecord> tempWriter;
        try {
            tempWriter = new SlsLogWriter();
            tempWriter.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        } catch (AliyunSLSDisabled e) {
            System.err.println("Aliyun SLS Disabled, fallback to FallbackQueuedLogWriter");
            tempWriter = new FallbackQueuedLogWriter();
        }
        this.writer = tempWriter;
    }

    public static SlsRecorderFactory getInstance() {
        return INSTANCE;
    }

    private QueuedLogWriter<LoggingRecord> writer() {
        return writer;
    }

    @Override
    public EventRecorder<LoggingRecord> createEventLogRecorder(@Nonnull String topic) {
        return new SlsEventRecorder(topic, writer());
    }

    @Override
    public <L extends IssueRecord<L>> IssueRecorder<L, LoggingRecord> createIssueRecorder(@Nonnull String topic, @Nonnull Supplier<L> issueRecordSupplier) {
        return new SlsIssueRecorder<>(topic, issueRecordSupplier, writer());
    }

    private static class FallbackQueuedLogWriter extends QueuedLogWriter<LoggingRecord> {
        private final Map<String, LoggingRecorder> logRecordMap = new ConcurrentHashMap<>();

        @Nonnull
        @Override
        protected Future<Void> processLogRecords(@Nonnull String topic, @Nonnull List<LoggingRecord> batch) {
            batch.forEach(item -> {
                logRecordMap.computeIfAbsent(topic, LoggingRecorder::embedded)
                            .recordLog(item);
            });
            return Future.succeededFuture();
        }
    }
}
