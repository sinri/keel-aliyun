package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.logger.api.logger.BaseLogger;
import org.jetbrains.annotations.NotNull;

/**
 * 基于阿里云日志服务的日志记录器。
 *
 * @since 5.0.0
 */
public class SlsLogger extends BaseLogger {

    public SlsLogger(@NotNull String topic, @NotNull QueuedLogWriterAdapter logWriterAdapter) {
        super(topic, logWriterAdapter);
    }

}
