package io.github.sinri.keel.integration.aliyun.sae;


import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 阿里云 SAE 任务模板任务执行时环境变量读取封装。
 *
 * @since 5.0.0
 */
@NullMarked
public class SAETaskUnitEnvReader {

    public String getInstanceId() {
        String instanceId = System.getenv("INSTANCE_ID");
        return Objects.requireNonNull(instanceId);
    }
}
