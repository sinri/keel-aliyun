package io.github.sinri.keel.integration.aliyun.sae;

import io.github.sinri.keel.base.logger.factory.StdoutLoggerFactory;
import io.github.sinri.keel.logger.api.factory.LoggerFactory;
import io.github.sinri.keel.logger.api.metric.MetricRecorder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 阿里云 SAE 任务模板用的任务执行类。
 *
 * @since 5.0.0
 */
@NullMarked
public interface SAETaskUnit {
    /**
     * 默认直接输出到标准输出，如有特殊需要可重载。
     *
     * @return 日志记录器工厂实例
     */
    default LoggerFactory getLoggerFactory() {
        return StdoutLoggerFactory.getInstance();
    }

    default @Nullable MetricRecorder getMetricRecorder() {
        return null;
    }

    /**
     * 应当在 main 方法中调用此方法来启动。
     *
     * @param args 程序入参
     */
    void launch(String[] args);

    /**
     * 处理异常，应当在 {@link SAETaskUnit#launch(String[])} 方法中适当地捕获导致运行非正常结束的异常，调用此方法来处理异常。
     *
     * @param throwable 导致运行非正常结束的异常
     */
    void handleError(Throwable throwable);

    SAETaskUnitEnvReader getEnvReader();
}
