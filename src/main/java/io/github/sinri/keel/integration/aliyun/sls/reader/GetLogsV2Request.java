package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.json.JsonObjectConvertible;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;

/**
 * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">GetLogsV2 -
 *         查询Logstore中的日志数据</a>
 */
@NullMarked
public class GetLogsV2Request implements JsonObjectConvertible {
    @Override
    public JsonObject toJsonObject() {
        // todo
    }

    @Override
    public final String toJsonExpression() {
        return toJsonObject().encode();
    }

    @Override
    public final String toFormattedJsonExpression() {
        return toJsonObject().encodePrettily();
    }
}
