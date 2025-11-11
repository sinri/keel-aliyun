package io.github.sinri.keel.logger.metric;

import io.github.sinri.keel.core.json.JsonObjectConvertible;
import io.github.sinri.keel.logger.api.issue.IssueRecord;
import io.github.sinri.keel.logger.api.metric.MetricRecord;
import io.vertx.core.json.JsonObject;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.1.9 Technical Preview
 * @since 3.2.0 extends BaseIssueRecord
 *         It is allowed to override this class, for fixed topic and metric.
 */
public class MetricRecordImpl extends IssueRecord<MetricRecordImpl>
        implements MetricRecord<MetricRecordImpl>, JsonObjectConvertible {
    private final @Nonnull Map<String, String> labelMap = new HashMap<>();
    private final @Nonnull String metricName;
    private final double value;

    public MetricRecordImpl(@Nonnull String metricName, double value) {
        super();
        this.metricName = metricName;
        this.value = value;
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

    @Override
    public MetricRecordImpl label(String name, String value) {
        this.labelMap.put(name, value);
        return this;
    }

    @Nonnull
    @Override
    public MetricRecordImpl getImplementation() {
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