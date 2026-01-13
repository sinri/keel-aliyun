package io.github.sinri.keel.integration.aliyun.sls.internal;

import io.github.sinri.keel.base.configuration.NotConfiguredException;
import io.github.sinri.keel.base.logger.metric.AbstractMetricRecorder;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSLSDisabled;
import io.github.sinri.keel.integration.aliyun.sls.AliyunSlsConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.internal.entity.LogItem;
import io.github.sinri.keel.logger.api.LateObject;
import io.github.sinri.keel.logger.api.metric.MetricRecord;
import io.vertx.core.Future;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 基于阿里云日志服务中的时序日志服务的定量指标记录器实现。
 *
 * @since 5.0.0
 */
@NullMarked
public class SlsMetricRecorder extends AbstractMetricRecorder {
    private final String source;
    private final AliyunSlsConfigElement aliyunSlsConfig;
    private final LateObject<AliyunSLSLogPutter> lateLogPutter = new LateObject<>();
    private final String project;
    private final String logstore;

    public SlsMetricRecorder(@Nullable AliyunSlsConfigElement aliyunSlsConfig) throws AliyunSLSDisabled {
        super();
        //        ConfigElement extract = keel.getConfiguration().extract("aliyun", "sls_metric");
        //        if (extract == null) {
        //            throw new AliyunSLSDisabled();
        //        }
        //aliyunSlsConfig = new AliyunSlsConfigElement(extract);
        if (aliyunSlsConfig == null || aliyunSlsConfig.isDisabled()) {
            throw new AliyunSLSDisabled();
        }
        this.aliyunSlsConfig = aliyunSlsConfig;

        this.source = AliyunSLSLogPutter.buildSource(aliyunSlsConfig.getSource());
        try {
            this.project = aliyunSlsConfig.getProject();
            this.logstore = aliyunSlsConfig.getLogstore();
            //this.logPutter = this.buildProducer();
        } catch (NotConfiguredException e) {
            throw new AliyunSLSDisabled(e.getMessage());
        }

        // after initialized, do not forget to deploy it.
    }

    private AliyunSLSLogPutter buildProducer() throws NotConfiguredException {
        return new AliyunSLSLogPutter(
                getVertx(),
                aliyunSlsConfig.getAccessKeyId(),
                aliyunSlsConfig.getAccessKeySecret(),
                aliyunSlsConfig.getEndpoint()
        );
    }

    @Override
    protected Future<Void> prepareForLoop() {
        AliyunSLSLogPutter aliyunSLSLogPutter;
        try {
            aliyunSLSLogPutter = buildProducer();
        } catch (NotConfiguredException e) {
            return Future.failedFuture(e);
        }
        lateLogPutter.set(aliyunSLSLogPutter);
        return Future.succeededFuture();
    }

    @Override
    protected Future<?> stopVerticle() {
        return super.stopVerticle()
                    .andThen(v -> {
                        lateLogPutter.get().close();
                    });
    }

    @Override
    protected Future<Void> handleForTopic(String topic, List<MetricRecord> buffer) {
        if (buffer.isEmpty()) {
            return Future.succeededFuture();
        }

        LogGroup logGroup = new LogGroup(topic, source);
        buffer.forEach(metricRecord -> {
            var logItem = buildLogItem(metricRecord);
            logGroup.addLogItem(logItem);
        });

        return lateLogPutter.get().putLogs(project, logstore, logGroup);
    }

    /**
     * metricName: the metric name, eg: http_requests_count
     * labels: labels map, eg: {'idc': 'idc1', 'ip': '192.0.2.0', 'hostname':
     * 'appserver1'}
     * value: double value, eg: 1.234
     *
     * @return LogItem
     */
    private LogItem buildLogItem(MetricRecord metricRecord) {
        String labelsKey = "__labels__";
        String timeKey = "__time_nano__";
        String valueKey = "__value__";
        String nameKey = "__name__";

        int timeInSec = (int) (metricRecord.timestamp() / 1000);
        LogItem logItem = new LogItem(timeInSec);
        logItem.addContent(timeKey, metricRecord.timestamp() + "000");
        logItem.addContent(nameKey, metricRecord.metricName());
        logItem.addContent(valueKey, String.valueOf(metricRecord.value()));

        // 按照字典序对labels排序, 如果您的labels已排序, 请忽略此步骤。
        metricRecord.labels().put("source", this.source);
        TreeMap<String, String> sortedLabels = new TreeMap<>(metricRecord.labels());
        StringBuilder labelsBuilder = new StringBuilder();

        boolean hasPrev = false;
        for (Map.Entry<String, String> entry : sortedLabels.entrySet()) {
            if (hasPrev) {
                labelsBuilder.append("|");
            }
            hasPrev = true;
            labelsBuilder.append(entry.getKey());
            labelsBuilder.append("#$#");
            labelsBuilder.append(entry.getValue());
        }
        logItem.addContent(labelsKey, labelsBuilder.toString());
        return logItem;
    }
}
