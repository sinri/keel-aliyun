package io.github.sinri.keel.integration.aliyun.sae;

/**
 * 阿里云 SAE 任务模板任务执行时环境变量读取封装。
 *
 * @since 5.0.0
 */
public class SAETaskUnitEnvReader {
    public String getInstanceId() {
        return System.getenv("INSTANCE_ID");
    }
}
