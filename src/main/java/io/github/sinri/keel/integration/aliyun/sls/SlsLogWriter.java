package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.configuration.KeelConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogItem;
import io.github.sinri.keel.logger.adapter.writer.QueuedLogWriter;
import io.github.sinri.keel.logger.api.record.LoggingRecord;
import io.vertx.core.Future;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

public class SlsLogWriter extends QueuedLogWriter<LoggingRecord> {
    private final String source;
    private final AliyunSlsConfigElement aliyunSlsConfig;
    private final int bufferSize;
    @Nonnull
    private final AliyunSLSLogPutter logPutter;

    public SlsLogWriter() throws AliyunSLSDisabled {
        this(128);
    }

    public SlsLogWriter(int bufferSize) throws AliyunSLSDisabled {
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
    protected Future<Void> processLogRecords(@Nonnull String topic, @Nonnull List<LoggingRecord> batch) {
        AtomicReference<LogGroup> currentLogGroupRef = new AtomicReference<>(new LogGroup(topic, source));

        return Keel.asyncCallIteratively(batch, eventLog -> {
                       int timeInSec = (int) (eventLog.timestamp() / 1000);
                       LogItem logItem = new LogItem(timeInSec);
                       eventLog.contents().forEach(content -> {
                           logItem.addContent(content.key(), content.value());
                       });

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

    @Override
    public void close() {
        super.close();
    }
}
