package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.Keel;
import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.base.configuration.ConfigTree;
import io.github.sinri.keel.base.json.JsonifiedThrowable;
import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSLSDisabled;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogItem;
import io.github.sinri.keel.logger.api.log.Log;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于队列处理的持久性日志写入适配器实现，将日志写入阿里云日志服务中。
 *
 * @since 5.0.0
 */
public class SlsQueuedLogWriterAdapter extends QueuedLogWriterAdapter {
    @NotNull
    private final String source;
    @NotNull
    private final AliyunSlsConfigElement aliyunSlsConfig;
    private final int bufferSize;
    @NotNull
    private final AliyunSLSLogPutter logPutter;
    private final @NotNull String project;
    private final @NotNull String logstore;

    public SlsQueuedLogWriterAdapter(@NotNull Keel keel) throws AliyunSLSDisabled {
        this(keel, 128);
    }

    public SlsQueuedLogWriterAdapter(@NotNull Keel keel, int bufferSize) throws AliyunSLSDisabled {
        super(keel);
        this.bufferSize = bufferSize;

        ConfigElement extract = keel.getConfiguration().extract("aliyun", "sls");
        if (extract == null) {
            throw new AliyunSLSDisabled();
        }

        aliyunSlsConfig = new AliyunSlsConfigElement(extract);
        if (aliyunSlsConfig.isDisabled()) {
            throw new AliyunSLSDisabled();
        }

        this.source = AliyunSLSLogPutter.buildSource(aliyunSlsConfig.getSource());
        try {
            this.project = aliyunSlsConfig.getProject();
            this.logstore = aliyunSlsConfig.getLogstore();
            this.logPutter = this.buildProducer();
        } catch (ConfigTree.NotConfiguredException e) {
            throw new RuntimeException(e);
        }

        // after initialized, do not forget to deploy it.
    }

    @Override
    protected @NotNull Future<Void> processLogRecords(@NotNull String topic, @NotNull List<SpecificLog<?>> batch) {
        AtomicReference<LogGroup> currentLogGroupRef = new AtomicReference<>(new LogGroup(topic, source));

        return getKeel().asyncCallIteratively(batch, specificLog -> {
                            int timeInSec = (int) (specificLog.timestamp() / 1000);
                            LogItem logItem = new LogItem(timeInSec);

                            String name = specificLog.level().name();
                            logItem.addContent(Log.MapKeyLevel, name);
                            String message = specificLog.message();
                            if (message != null) {
                                logItem.addContent(Log.MapKeyMessage, message);
                            }
                            List<String> classification = specificLog.classification();
                            if (classification != null && !classification.isEmpty()) {
                                logItem.addContent(Log.MapKeyClassification, new JsonArray(classification).encode());
                            }
                            Throwable exception = specificLog.exception();
                            if (exception != null) {
                                logItem.addContent(Log.MapKeyException, JsonifiedThrowable.wrap(exception)
                                                                                          .toJsonExpression());
                            }
                            Map<String, Object> context = specificLog.context().toMap();
                            if (!context.isEmpty()) {
                                logItem.addContent(Log.MapKeyContext, new JsonObject(context).encode());
                            }
                            Map<String, Object> extra = specificLog.extra();
                            if (!context.isEmpty()) {
                                extra.forEach((k, v) -> {
                                    if (v != null) {
                                        logItem.addContent(k, v.toString());
                                    }
                                });
                            }

                            LogGroup currentLogGroup = currentLogGroupRef.get();
                            currentLogGroup.addLogItem(logItem);
                            if (currentLogGroup.getProbableSize() > 5 * 1024 * 1024) {
                                return this.logPutter.putLogs(project, logstore, currentLogGroup)
                                                     .compose(v -> {
                                                         currentLogGroupRef.set(new LogGroup(topic, source));
                                                         return Future.succeededFuture();
                                                     });
                            } else {
                                return Future.succeededFuture();
                            }
                        })
                        .compose(v -> {
                            LogGroup currentLogGroup = currentLogGroupRef.get();
                            if (currentLogGroup.getProbableSize() > 0) {
                                return this.logPutter.putLogs(project, logstore, currentLogGroup);
                            } else {
                                return Future.succeededFuture();
                            }
                        });
    }

    @NotNull
    private AliyunSLSLogPutter buildProducer() throws ConfigTree.NotConfiguredException {
        return new AliyunSLSLogPutter(
                getVertx(),
                aliyunSlsConfig.getAccessKeyId(),
                aliyunSlsConfig.getAccessKeySecret(),
                aliyunSlsConfig.getEndpoint()
        );
    }

    @Override
    protected int bufferSize() {
        return bufferSize;
    }
}
