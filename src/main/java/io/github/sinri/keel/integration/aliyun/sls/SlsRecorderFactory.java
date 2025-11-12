package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.logger.api.consumer.TopicRecordConsumer;
import io.github.sinri.keel.logger.api.event.EventRecord;
import io.github.sinri.keel.logger.api.event.EventRecorder;
import io.github.sinri.keel.logger.api.factory.RecorderFactory;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.issue.IssueRecorder;
import io.github.sinri.keel.logger.base.event.BaseEventRecorder;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SlsRecorderFactory implements RecorderFactory {
    private static final SlsRecorderFactory INSTANCE = new SlsRecorderFactory();
    private final QueuedTopicRecordConsumer consumer;

    private SlsRecorderFactory() {
        QueuedTopicRecordConsumer tempWriter;
        try {
            tempWriter = new SlsQueuedTopicRecordConsumer();
            tempWriter.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        } catch (AliyunSLSDisabled e) {
            System.err.println("Aliyun SLS Disabled, fallback to FallbackQueuedLogWriter");
            tempWriter = new FallbackQueuedLogWriter();
        }
        this.consumer = tempWriter;
    }

    public static SlsRecorderFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public EventRecorder createEventRecorder(@Nonnull String topic) {
        return new SlsEventRecorder(topic, consumer);
    }

    @Override
    public TopicRecordConsumer sharedTopicRecordConsumer() {
        return consumer;
    }

    @Override
    public <L extends IssueRecord<L>> IssueRecorder<L> createIssueRecorder(@Nonnull String topic, @Nonnull Supplier<L> issueRecordSupplier) {
        return new SlsIssueRecorder<>(topic, issueRecordSupplier, consumer);
    }

    private static class FallbackQueuedLogWriter extends QueuedTopicRecordConsumer {
        private final Map<String, EventRecorder> logRecordMap = new ConcurrentHashMap<>();

        @Nonnull
        @Override
        protected Future<Void> processLogRecords(@Nonnull String topic, @Nonnull List<EventRecord> batch) {
            batch.forEach(item -> {
                logRecordMap.computeIfAbsent(topic, BaseEventRecorder::new)
                            .recordEvent(item);
            });
            return Future.succeededFuture();
        }
    }
}
