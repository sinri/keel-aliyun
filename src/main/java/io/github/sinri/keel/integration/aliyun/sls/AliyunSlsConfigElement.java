package io.github.sinri.keel.integration.aliyun.sls;


import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.base.configuration.ConfigTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 阿里云日志服务相关配置节点。
 *
 * @since 5.0.0
 */
public class AliyunSlsConfigElement extends ConfigTree {

    // Configuration keys
    private static final String CONFIG_KEY_DISABLED = "disabled";
    private static final String CONFIG_KEY_PROJECT = "project";
    private static final String CONFIG_KEY_LOGSTORE = "logstore";
    private static final String CONFIG_KEY_SOURCE = "source";
    private static final String CONFIG_KEY_ENDPOINT = "endpoint";
    private static final String CONFIG_KEY_ACCESS_KEY_ID = "accessKeyId";
    private static final String CONFIG_KEY_ACCESS_KEY_SECRET = "accessKeySecret";

    public AliyunSlsConfigElement(@NotNull ConfigElement another) {
        super(another);
    }

    public final boolean isDisabled() {
        try {
            return readBoolean(List.of(CONFIG_KEY_DISABLED));
        } catch (NotConfiguredException e) {
            return false;
        }
    }

    @NotNull
    public final String getProject() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_PROJECT));
    }

    @NotNull
    public final String getLogstore() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_LOGSTORE));
    }

    @Nullable
    public final String getSource() {
        try {
            return readString(List.of(CONFIG_KEY_SOURCE));
        } catch (NotConfiguredException e) {
            return null;
        }
    }

    @NotNull
    public final String getEndpoint() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ENDPOINT));
    }

    @NotNull
    public final String getAccessKeyId() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ACCESS_KEY_ID));
    }

    @NotNull
    public final String getAccessKeySecret() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ACCESS_KEY_SECRET));
    }
}
