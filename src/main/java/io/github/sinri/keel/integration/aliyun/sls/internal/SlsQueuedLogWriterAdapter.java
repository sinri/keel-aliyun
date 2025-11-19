package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.base.json.JsonifiedThrowable;
import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSLSDisabled;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogItem;
import io.github.sinri.keel.logger.api.log.Log;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.base.KeelInstance.Keel;

/**
 * 基于队列处理的持久性日志写入适配器实现，将日志写入阿里云日志服务中。
 *
 * @since 5.0.0
 */
public class SlsQueuedLogWriterAdapter extends QueuedLogWriterAdapter {
    private final String source;
    private final AliyunSlsConfigElement aliyunSlsConfig;
    private final int bufferSize;
    @NotNull
    private final AliyunSLSLogPutter logPutter;

    public SlsQueuedLogWriterAdapter() throws AliyunSLSDisabled {
        this(128);
    }

    public SlsQueuedLogWriterAdapter(int bufferSize) throws AliyunSLSDisabled {
        this.bufferSize = bufferSize;

        ConfigElement extract = Keel.getConfiguration().extract("aliyun", "sls");
        if (extract == null) {
            throw new AliyunSLSDisabled();
        }

        aliyunSlsConfig = new AliyunSlsConfigElement(extract);
        if (aliyunSlsConfig.isDisabled()) {
            throw new AliyunSLSDisabled();
        }

        this.source = AliyunSLSLogPutter.buildSource(aliyunSlsConfig.getSource());
        this.logPutter = this.buildProducer();

        // after initialized, do not forget to deploy it.
    }


    @NotNull
    @Override
    protected Future<Void> processLogRecords(@NotNull String topic, @NotNull List<Log> batch) {
        AtomicReference<LogGroup> currentLogGroupRef = new AtomicReference<>(new LogGroup(topic, source));

        return Keel.asyncCallIteratively(batch, eventLog -> {
                       int timeInSec = (int) (eventLog.timestamp() / 1000);
                       LogItem logItem = new LogItem(timeInSec);

                       String name = eventLog.level().name();
                       logItem.addContent(Log.MapKeyLevel, name);
                       logItem.addContent(Log.MapKeyMessage, eventLog.message());
                       List<String> classification = eventLog.classification();
                       if (classification != null && !classification.isEmpty()) {
                           logItem.addContent(Log.MapKeyClassification, new JsonArray(classification).encode());
                       }
                       Throwable exception = eventLog.exception();
                       if (exception != null) {
                           logItem.addContent(Log.MapKeyException, JsonifiedThrowable.wrap(exception)
                                                                                             .toJsonExpression());
                       }
                       Map<String, Object> context = eventLog.context().toMap();
                       if (!context.isEmpty()) {
                           logItem.addContent(Log.MapKeyContext, new JsonObject(context).encode());
                       }
                       Map<String, Object> extra = eventLog.extra();
                       if (!context.isEmpty()) {
                           extra.forEach((k, v) -> {
                               logItem.addContent(k, v == null ? null : v.toString());
                           });
                       }

                       LogGroup currentLogGroup = currentLogGroupRef.get();
                       currentLogGroup.addLogItem(logItem);
                       if (currentLogGroup.getProbableSize() > 5 * 1024 * 1024) {
                           return this.logPutter.putLogs(aliyunSlsConfig.getProject(), aliyunSlsConfig.getLogstore(), currentLogGroup)
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
                           return this.logPutter.putLogs(aliyunSlsConfig.getProject(), aliyunSlsConfig.getLogstore(), currentLogGroup);
                       } else {
                           return Future.succeededFuture();
                       }
                   });
    }

    private AliyunSLSLogPutter buildProducer() {
        return new AliyunSLSLogPutter(
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
