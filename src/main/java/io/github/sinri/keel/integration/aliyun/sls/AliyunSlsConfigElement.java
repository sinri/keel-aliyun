package io.github.sinri.keel.integration.aliyun.sls;


import io.github.sinri.keel.base.configuration.ConfigElement;
import io.github.sinri.keel.base.configuration.NotConfiguredException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 阿里云日志服务相关配置节点。
 *
 * @since 5.0.0
 */
@NullMarked
public class AliyunSlsConfigElement extends ConfigElement {

    // Configuration keys
    private static final String CONFIG_KEY_DISABLED = "disabled";
    private static final String CONFIG_KEY_PROJECT = "project";
    private static final String CONFIG_KEY_LOGSTORE = "logstore";
    private static final String CONFIG_KEY_SOURCE = "source";
    private static final String CONFIG_KEY_ENDPOINT = "endpoint";
    private static final String CONFIG_KEY_ACCESS_KEY_ID = "accessKeyId";
    private static final String CONFIG_KEY_ACCESS_KEY_SECRET = "accessKeySecret";

    public AliyunSlsConfigElement(ConfigElement another) {
        super(another);
    }

    public final boolean isDisabled() {
        try {
            return readBoolean(List.of(CONFIG_KEY_DISABLED));
        } catch (NotConfiguredException e) {
            return false;
        }
    }

    public final String getProject() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_PROJECT));
    }

    public final String getLogstore() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_LOGSTORE));
    }

    public final @Nullable String getSource() {
        try {
            return readString(List.of(CONFIG_KEY_SOURCE));
        } catch (NotConfiguredException e) {
            return null;
        }
    }

    public final String getEndpoint() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ENDPOINT));
    }

    public final String getAccessKeyId() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ACCESS_KEY_ID));
    }

    public final String getAccessKeySecret() throws NotConfiguredException {
        return readString(List.of(CONFIG_KEY_ACCESS_KEY_SECRET));
    }
}
