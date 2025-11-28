package io.github.sinri.keel.integration.aliyun.sls;

import org.jetbrains.annotations.NotNull;

/**
 * 当阿里云的日志服务被禁用时抛出的异常。
 *
 * @since 5.0.0
 */
public final class AliyunSLSDisabled extends Exception {
    public AliyunSLSDisabled() {
        super("Aliyun SLS logging is disabled");
    }

    public AliyunSLSDisabled(@NotNull String message) {
        super("Aliyun SLS logging is disabled due to " + message);
    }
}
