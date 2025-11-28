package io.github.sinri.keel.integration.aliyun.sls.internal.entity;

import com.google.protobuf.DynamicMessage;
import io.github.sinri.keel.integration.aliyun.sls.internal.protocol.LogEntityDescriptors;
import org.jetbrains.annotations.NotNull;

/**
 * @see <a href=
 *         "https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-struct-logcontent">LogContent</a>
 * @since 5.0.0
 */
public class LogContent {
    @NotNull
    private final String key;
    @NotNull
    private final String value;

    public LogContent(@NotNull String key, @NotNull String value) {
        this.key = key;
        this.value = value;
    }

    public @NotNull String getKey() {
        return key;
    }

    public @NotNull String getValue() {
        return value;
    }

    @NotNull
    public DynamicMessage toProtobuf() {
        var contentDescriptor = LogEntityDescriptors.getInstance().getContentDescriptor();
        return DynamicMessage.newBuilder(contentDescriptor)
                             .setField(contentDescriptor.findFieldByName("Key"), key)
                             .setField(contentDescriptor.findFieldByName("Value"), value)
                             .build();
    }

    /**
     * Calculates and returns the probable size of the log content in bytes.
     * <p>
     * 使用UTF-8编码的平均字符长度3倍作为估计值。
     *
     * @return The total size in bytes of the UTF-8 encoded key and value combined.
     */
    public int getProbableSize() {
        return (key.length() + value.length()) * 3;
    }
}
