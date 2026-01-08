package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.logger.adapter.QueuedLogWriterAdapter;
import io.github.sinri.keel.logger.api.log.SpecificLog;
import io.github.sinri.keel.logger.api.logger.BaseSpecificLogger;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

/**
 * 基于阿里云日志服务的特定日志记录器。
 *
 * @param <T> 特定日志记录的类型
 * @since 5.0.0
 */
@NullMarked
public class SlsSpecificLogger<T extends SpecificLog<T>> extends BaseSpecificLogger<T> {

    public SlsSpecificLogger(String topic, Supplier<T> specificLogSupplier, QueuedLogWriterAdapter queuedLogWriterAdapter) {
        super(topic, specificLogSupplier, queuedLogWriterAdapter);
    }

}
