package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsIssueRecorder;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsLogger;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsQueuedLogWriterAdapter;
import io.github.sinri.keel.logger.api.LogLevel;
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

/**
 * 基于阿里云日志服务的日志记录器工厂。
 * <p>
 * 在确保配置加载完成之后再新建实例。
 *
 * @since 5.0.0
 */
public class SlsRecorderFactory implements LoggerFactory {
    @NotNull
    private final QueuedLogWriterAdapter adapter;

    public SlsRecorderFactory(@NotNull Keel keel) {
        QueuedLogWriterAdapter tempWriter;
        try {
            tempWriter = new SlsQueuedLogWriterAdapter(keel);
        } catch (AliyunSLSDisabled e) {
            tempWriter = buildFallbackQueuedLogWriter(keel);
            tempWriter.accept(getClass().getName(), new Log()
                    .level(LogLevel.WARNING)
                    .message("Aliyun SLS Disabled, fallback to " + tempWriter.getClass().getName()));
        }
        tempWriter.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        this.adapter = tempWriter;
    }

    @NotNull
    protected QueuedLogWriterAdapter buildFallbackQueuedLogWriter(@NotNull Keel keel) {
        return new FallbackQueuedLogWriter(keel);
    }

    @Override
    public @NotNull Logger createLogger(@NotNull String topic) {
        return new SlsLogger(topic, adapter);
    }

    @Override
    public @NotNull LogWriterAdapter sharedAdapter() {
        return adapter;
    }

    @Override
    public <L extends SpecificLog<L>> @NotNull SpecificLogger<L> createLogger(@NotNull String topic, @NotNull Supplier<L> issueRecordSupplier) {
        return new SlsIssueRecorder<>(topic, issueRecordSupplier, adapter);
    }

    private static class FallbackQueuedLogWriter extends QueuedLogWriterAdapter {
        @NotNull
        private final Map<String, Logger> logRecordMap = new ConcurrentHashMap<>();

        public FallbackQueuedLogWriter(@NotNull Keel keel) {
            super(keel);
        }

        @NotNull
        private static Log formatLog(@NotNull SpecificLog<?> specificLog) {
            if (specificLog instanceof Log log) {
                return log;
            } else {
                return new Log(specificLog);
            }
        }

        @Override
        protected @NotNull Future<Void> processLogRecords(@NotNull String topic, @NotNull List<SpecificLog<?>> batch) {
            batch.forEach(item -> {
                Logger logger = logRecordMap.computeIfAbsent(topic, s -> new BaseLogger(s, FallbackQueuedLogWriter.this));
                logger.log(formatLog(item));
            });
            return Future.succeededFuture();
        }
    }
}
