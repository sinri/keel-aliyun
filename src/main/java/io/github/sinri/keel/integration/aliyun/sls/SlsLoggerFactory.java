package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.base.verticles.KeelVerticleBase;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsLogger;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsQueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.internal.SlsSpecificLogger;
import io.github.sinri.keel.logger.api.LateObject;
import io.github.sinri.keel.logger.api.LogLevel;
import io.github.sinri.keel.logger.api.adapter.LogWriterAdapter;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.log.Log;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.github.sinri.keel.logger.api.logger.Logger;
import io.github.sinri.keel.logger.api.logger.SpecificLogger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.ThreadingModel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 基于阿里云日志服务的日志记录器工厂。
 * <p>
 * 在确保配置加载完成之后再新建实例。
 *
 * @since 5.0.0
 */
@NullMarked
public class SlsLoggerFactory extends KeelVerticleBase implements LoggerFactory {
    private final @Nullable AliyunSlsConfigElement aliyunSlsConfig;
    private final LateObject<QueuedLogWriterAdapter> lateAdapter = new LateObject<>();

    public SlsLoggerFactory(@Nullable AliyunSlsConfigElement aliyunSlsConfig) {
        this.aliyunSlsConfig = aliyunSlsConfig;
    }

    @Override
    protected Future<Void> startVerticle() {
        //        System.out.println("SlsLoggerFactory starting...");
        QueuedLogWriterAdapter tempWriter;
        try {
            tempWriter = new SlsQueuedLogWriterAdapter(aliyunSlsConfig);
        } catch (AliyunSLSDisabled e) {
            System.out.println("Aliyun SLS Disabled, use fallback");
            tempWriter = buildFallbackQueuedLogWriter();
            tempWriter.accept(getClass().getName(), new Log()
                    .level(LogLevel.WARNING)
                    .message("SlsLoggerFactory adapter impl fallback to " + tempWriter.getClass().getName()));
        }
        this.lateAdapter.set(tempWriter);

        return this.lateAdapter
                .get()
                .deployMe(getVertx(), new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
                .andThen(ar -> {
                    if (ar.failed()) {
                        System.err.println("Failed to deploy SlsQueuedLogWriterAdapter: " + ar.cause());
                    } else {
                        System.out.println("SlsQueuedLogWriterAdapter deployed: " + ar.result());
                    }
                })
                .compose(s -> {
                    return Future.succeededFuture();
                });
    }

    @Override
    protected Future<Void> stopVerticle() {
        if (lateAdapter.isInitialized()) {
            return lateAdapter.get().undeployMe();
        }
        return Future.succeededFuture();
    }

    protected QueuedLogWriterAdapter buildFallbackQueuedLogWriter() {
        return new FallbackQueuedLogWriter();
    }

    @Override
    public Logger createLogger(String topic) {
        return new SlsLogger(topic, lateAdapter.get());
    }

    @Override
    public LogWriterAdapter sharedAdapter() {
        return lateAdapter.get();
    }

    @Override
    public <L extends SpecificLog<L>> SpecificLogger<L> createLogger(String topic, Supplier<L> specificLogSupplier) {
        return new SlsSpecificLogger<>(topic, specificLogSupplier, lateAdapter.get());
    }

}
