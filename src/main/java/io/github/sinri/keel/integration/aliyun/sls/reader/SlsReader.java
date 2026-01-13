package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.VertxHolder;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * 本类提供阿里云日志服务（SLS）的日志获取类功能。
 * 关于 SLS 公共接口调用，基于 V3 版本的签名机制实现。
 *
 * @see <a href="https://help.aliyun.com/zh/sdk/product-overview/v3-request-structure-and-signature">V3版本请求体 and
 *         签名机制</a>
 */
public class SlsReader implements VertxHolder {
    private final Vertx vertx;
    private final AliyunSlsConfigElement aliyunSlsConfigElement;

    public SlsReader(Vertx vertx, AliyunSlsConfigElement aliyunSlsConfigElement) {
        this.vertx = vertx;
        this.aliyunSlsConfigElement = aliyunSlsConfigElement;
    }

    @Override
    public final Vertx getVertx() {
        return vertx;
    }

    /**
     * 查询指定Project下某个Logstore中的原始日志数据，返回结果显示某时间区间中的原始日志（返回结果压缩后传输）。
     *
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">GetLogsV2</a>
     */
    public Future<JsonObject> callGetLogsV2(GetLogsV2Request request) {
        // todo
    }
}
