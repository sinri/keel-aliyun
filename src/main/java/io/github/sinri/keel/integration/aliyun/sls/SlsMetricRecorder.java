package io.github.sinri.keel.integration.aliyun.sls;

import io.github.sinri.keel.base.configuration.KeelConfigElement;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogGroup;
import io.github.sinri.keel.integration.aliyun.sls.entity.LogItem;
import io.github.sinri.keel.logger.api.metric.MetricRecord;
import io.github.sinri.keel.logger.metric.AbstractMetricRecorder;
import io.vertx.core.Future;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.github.sinri.keel.base.KeelInstance.Keel;


public class SlsMetricRecorder extends AbstractMetricRecorder {
    private final String source;
    private final AliyunSlsConfigElement aliyunSlsConfig;
    @NotNull
    private final AliyunSLSLogPutter logPutter;

    public SlsMetricRecorder() throws AliyunSLSDisabled {
        super();
        KeelConfigElement extract = Keel.getConfiguration().extract("aliyun", "sls_metric");
        if (extract == null) {
            throw new AliyunSLSDisabled();
        }
        aliyunSlsConfig = new AliyunSlsConfigElement(extract);
        if (aliyunSlsConfig.isDisabled()) {
            throw new AliyunSLSDisabled();
        }

        this.source = AliyunSLSLogPutter.buildSource(aliyunSlsConfig.getSource());
        this.logPutter = this.buildProducer();

        // after initialized, do not forget to deploy it.
    }

    @NotNull
    private AliyunSLSLogPutter buildProducer() {
        return new AliyunSLSLogPutter(
                aliyunSlsConfig.getAccessKeyId(),
                aliyunSlsConfig.getAccessKeySecret(),
                aliyunSlsConfig.getEndpoint()
        );
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

        return logPutter.putLogs(
                aliyunSlsConfig.getProject(),
                aliyunSlsConfig.getLogstore(),
                logGroup
        );
    }
    /**
     * metricName: the metric name, eg: http_requests_count
     * labels: labels map, eg: {'idc': 'idc1', 'ip': '192.0.2.0', 'hostname':
     * 'appserver1'}
     * value: double value, eg: 1.234
     *
     * @return LogItem
     */
    private LogItem buildLogItem(@NotNull MetricRecord metricRecord) {
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
