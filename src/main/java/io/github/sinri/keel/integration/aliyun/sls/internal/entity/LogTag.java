package io.github.sinri.keel.integration.aliyun.sls.internal.entity;

import com.google.protobuf.DynamicMessage;
import io.github.sinri.keel.integration.aliyun.sls.internal.protocol.LogEntityDescriptors;
import org.jetbrains.annotations.NotNull;

/**
 * @see <a
 *         href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-struct-logtag">LogTag</a>
 * @since 5.0.0
 */
public class LogTag {
    private @NotNull String key;
    private @NotNull String value;

    /**
     * Create a LogTag with key and value
     *
     * @param key   The tag key
     * @param value The tag value
     */
    public LogTag(@NotNull String key, @NotNull String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Get the tag key
     *
     * @return The tag key
     */
    @NotNull
    public String getKey() {
        return key;
    }

    /**
     * Set the tag key
     *
     * @param key The tag key to set
     * @return this instance for chaining
     */
    public LogTag setKey(@NotNull String key) {
        this.key = key;
        return this;
    }

    /**
     * Get the tag value
     *
     * @return The tag value
     */
    @NotNull
    public String getValue() {
        return value;
    }

    /**
     * Set the tag value
     *
     * @param value The tag value to set
     * @return this instance for chaining
     */
    @NotNull
    public LogTag setValue(@NotNull String value) {
        this.value = value;
        return this;
    }

    @NotNull
    public DynamicMessage toProtobuf() {
        var logTagDescriptor = LogEntityDescriptors.getInstance().getLogTagDescriptor();
        return DynamicMessage.newBuilder(logTagDescriptor)
                             .setField(logTagDescriptor.findFieldByName("Key"), key)
                             .setField(logTagDescriptor.findFieldByName("Value"), value)
                             .build();
    }
}
