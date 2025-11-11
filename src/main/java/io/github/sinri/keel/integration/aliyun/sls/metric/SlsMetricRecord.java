package io.github.sinri.keel.integration.aliyun.sls.metric;

import io.github.sinri.keel.core.json.JsonObjectConvertible;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.metric.MetricRecord;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.1.9 Technical Preview
 * @since 3.2.0 extends BaseIssueRecord
 *         It is allowed to override this class, for fixed topic and metric.
 */
public class SlsMetricRecord extends IssueRecord<SlsMetricRecord>
        implements MetricRecord, JsonObjectConvertible {
    private final @Nonnull Map<String, String> labelMap = new HashMap<>();
    private final @Nonnull String metricName;
    private final double value;

    public SlsMetricRecord(@Nonnull String metricName, double value, @Nullable Map<String, String> labels) {
        super();
        this.metricName = metricName;
        this.value = value;
        if (labels != null) {
            this.labelMap.putAll(labels);
        }
    }

    @Nonnull
    @Override
    public JsonObject toJsonObject() {
        JsonObject labelObject = new JsonObject();
        labelMap.forEach(labelObject::put);
        return new JsonObject()
                .put("timestamp", timestamp())
                .put("labels", labelObject)
                .put("metric_name", metricName)
                .put("value", value);
    }


    @Override
    @Nonnull
    public String metricName() {
        return metricName;
    }

    @Override
    public double value() {
        return value;
    }

    @Override
    @Nonnull
    public Map<String, String> labels() {
        return labelMap;
    }

    @Nonnull
    @Override
    public SlsMetricRecord getImplementation() {
        return this;
    }

    @Override
    public String toJsonExpression() {
        return toJsonObject().encode();
    }

    @Override
    public String toFormattedJsonExpression() {
        return toJsonObject().encodePrettily();
    }
}