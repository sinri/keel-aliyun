package io.github.sinri.keel.integration.aliyun.sls;

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
import org.jetbrains.annotations.Nullable;

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
    private final QueuedLogWriterAdapter adapter;

    public SlsRecorderFactory() {
        QueuedLogWriterAdapter tempWriter;
        try {
            tempWriter = new SlsQueuedLogWriterAdapter();
        } catch (AliyunSLSDisabled e) {
            tempWriter = buildFallbackQueuedLogWriter();
            tempWriter.accept(getClass().getName(), new Log()
                    .level(LogLevel.WARNING)
                    .message("Aliyun SLS Disabled, fallback to " + tempWriter.getClass().getName()));
        }
        tempWriter.deployMe(new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER));
        this.adapter = tempWriter;
    }

    @NotNull
    protected QueuedLogWriterAdapter buildFallbackQueuedLogWriter() {
        return new FallbackQueuedLogWriter();
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

        private static Log formatLog(SpecificLog<?> specificLog) {
            var log = new ExtendedLog();
            log.level(specificLog.level());
            String message = specificLog.message();
            if (message != null) {
                log.message(message);
            }
            List<String> classification = specificLog.classification();
            if (classification != null) {
                log.classification(classification);
            }
            var context = specificLog.context().toMap();
            if (!context.isEmpty()) {
                context.forEach(log::context);
            }
            Throwable exception = specificLog.exception();
            if (exception != null) {
                log.exception(exception);
            }
            Map<String, Object> extra = specificLog.extra();
            if (!extra.isEmpty()) {
                extra.forEach(log::extra);
            }

            return log;
        }

        @Override
        protected @NotNull Future<Void> processLogRecords(@NotNull String topic, @NotNull List<SpecificLog<?>> batch) {
            batch.forEach(item -> {
                Logger logger = logRecordMap.computeIfAbsent(topic, s -> new BaseLogger(s, FallbackQueuedLogWriter.this));
                logger.log(formatLog(item));
            });
            return Future.succeededFuture();
        }

        private static class ExtendedLog extends Log {
            @Override
            public @NotNull Log extra(@NotNull String key, @Nullable Object value) {
                return super.extra(key, value);
            }
        }
    }
}
