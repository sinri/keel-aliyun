package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsIssueRecorder;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsLogger;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsQueuedLogWriterAdapter;
import io.github.sinri.keel.logger.api.adapter.LogWriterAdapter;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.log.Log;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.github.sinri.keel.logger.api.logger.BaseLogger;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SlsRecorderFactory implements LoggerFactory {
    private final QueuedLogWriterAdapter adapter;

    public SlsRecorderFactory() {
        QueuedLogWriterAdapter tempWriter;
        try {
            tempWriter = new SlsQueuedLogWriterAdapter();
            tempWriter.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        } catch (AliyunSLSDisabled e) {
            System.err.println("Aliyun SLS Disabled, fallback to FallbackQueuedLogWriter");
            tempWriter = new FallbackQueuedLogWriter();
        }
        this.adapter = tempWriter;
    }


    @Override
    public Logger createLogger(@NotNull String topic) {
        return new SlsLogger(topic, adapter);
    }

    @Override
    public LogWriterAdapter sharedAdapter() {
        return adapter;
    }

    @Override
    public <L extends SpecificLog<L>> SpecificLogger<L> createLogger(@NotNull String topic, @NotNull Supplier<L> issueRecordSupplier) {
        return new SlsIssueRecorder<>(topic, issueRecordSupplier, adapter);
    }

    private static class FallbackQueuedLogWriter extends QueuedLogWriterAdapter {
        private final Map<String, Logger> logRecordMap = new ConcurrentHashMap<>();

        @NotNull
        @Override
        protected Future<Void> processLogRecords(@NotNull String topic, @NotNull List<Log> batch) {
            batch.forEach(item -> {
                logRecordMap.computeIfAbsent(topic, BaseLogger::new)
                            .recordEvent(item);
            });
            return Future.succeededFuture();
        }
    }
}
