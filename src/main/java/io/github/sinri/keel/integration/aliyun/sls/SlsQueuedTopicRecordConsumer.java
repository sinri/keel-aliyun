package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.configuration.KeelConfigElement;
import io.github.sinri.keel.core.json.JsonifiedThrowable;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogItem;
import io.github.sinri.keel.logger.api.event.EventRecord;
import io.github.sinri.keel.logger.consumer.QueuedTopicRecordConsumer;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

public class SlsQueuedTopicRecordConsumer extends QueuedTopicRecordConsumer {
    private final String source;
    private final AliyunSlsConfigElement aliyunSlsConfig;
    private final int bufferSize;
    @Nonnull
    private final AliyunSLSLogPutter logPutter;

    public SlsQueuedTopicRecordConsumer() throws AliyunSLSDisabled {
        this(128);
    }

    public SlsQueuedTopicRecordConsumer(int bufferSize) throws AliyunSLSDisabled {
        this.bufferSize = bufferSize;

        KeelConfigElement extract = Keel.getConfiguration().extract("aliyun", "sls");
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


    @Nonnull
    @Override
    protected Future<Void> processLogRecords(@Nonnull String topic, @Nonnull List<EventRecord> batch) {
        AtomicReference<LogGroup> currentLogGroupRef = new AtomicReference<>(new LogGroup(topic, source));

        return Keel.asyncCallIteratively(batch, eventLog -> {
                       int timeInSec = (int) (eventLog.timestamp() / 1000);
                       LogItem logItem = new LogItem(timeInSec);

                       String name = eventLog.level().name();
                       logItem.addContent(EventRecord.MapKeyLevel, name);
                       logItem.addContent(EventRecord.MapKeyMessage, eventLog.message());
                       List<String> classification = eventLog.classification();
                       if (classification != null && !classification.isEmpty()) {
                           logItem.addContent(EventRecord.MapKeyClassification, new JsonArray(classification).encode());
                       }
                       Throwable exception = eventLog.exception();
                       if (exception != null) {
                           logItem.addContent(EventRecord.MapKeyException, JsonifiedThrowable.wrap(exception)
                                                                                             .toJsonExpression());
                       }
                       Map<String, Object> context = eventLog.context().toMap();
                       if (!context.isEmpty()) {
                           logItem.addContent(EventRecord.MapKeyContext, new JsonObject(context).encode());
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
