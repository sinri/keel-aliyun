package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.logger.api.logger.BaseLogger;
import org.jspecify.annotations.NullMarked;

/**
 * 基于阿里云日志服务的日志记录器。
 *
 * @since 5.0.0
 */
@NullMarked
public class SlsLogger extends BaseLogger {

    public SlsLogger(String topic, QueuedLogWriterAdapter logWriterAdapter) {
        super(topic, logWriterAdapter);
    }

}
