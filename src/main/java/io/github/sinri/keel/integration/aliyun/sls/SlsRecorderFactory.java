package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.base.verticles.KeelVerticleBase;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsLogger;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsQueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsSpecificLogger;
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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
@NullMarked
public class SlsRecorderFactory extends KeelVerticleBase implements LoggerFactory {
    private final QueuedLogWriterAdapter adapter;

    public SlsRecorderFactory(@Nullable AliyunSlsConfigElement aliyunSlsConfig) {
        QueuedLogWriterAdapter tempWriter;
        try {
            tempWriter = new SlsQueuedLogWriterAdapter(aliyunSlsConfig);
        } catch (AliyunSLSDisabled e) {
            tempWriter = buildFallbackQueuedLogWriter();
            tempWriter.accept(getClass().getName(), new Log()
                    .level(LogLevel.WARNING)
                    .message("Aliyun SLS Disabled, fallback to " + tempWriter.getClass().getName()));
        }
        this.adapter = tempWriter;
    }

    @Override
    protected Future<?> startVerticle() {
        return adapter.deployMe(getVertx(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                      .andThen(ar -> {
                          if (ar.failed()) {
                              System.err.println("Failed to deploy SlsQueuedLogWriterAdapter: " + ar.cause());
                          } else {
                              System.out.println("SlsQueuedLogWriterAdapter deployed: " + ar.result());
                          }
                      });
    }

    @Override
    protected Future<?> stopVerticle() {
        return adapter.undeployMe();
    }

    protected QueuedLogWriterAdapter buildFallbackQueuedLogWriter() {
        return new FallbackQueuedLogWriter();
    }

    @Override
    public Logger createLogger(String topic) {
        return new SlsLogger(topic, adapter);
    }

    @Override
    public LogWriterAdapter sharedAdapter() {
        return adapter;
    }

    @Override
    public <L extends SpecificLog<L>> SpecificLogger<L> createLogger(String topic, Supplier<L> specificLogSupplier) {
        return new SlsSpecificLogger<>(topic, specificLogSupplier, adapter);
    }

    @NullMarked
    private static class FallbackQueuedLogWriter extends QueuedLogWriterAdapter {
        private final Map<String, Logger> logRecordMap = new ConcurrentHashMap<>();

        public FallbackQueuedLogWriter() {
            super();
        }

        private static Log formatLog(SpecificLog<?> specificLog) {
            if (specificLog instanceof Log log) {
                return log;
            } else {
                return new Log(specificLog);
            }
        }

        @Override
        protected Future<Void> processLogRecords(String topic, List<SpecificLog<?>> batch) {
            batch.forEach(item -> {
                Logger logger = logRecordMap.computeIfAbsent(topic, s -> new BaseLogger(s, FallbackQueuedLogWriter.this));
                logger.log(formatLog(item));
            });
            return Future.succeededFuture();
        }
    }
}
