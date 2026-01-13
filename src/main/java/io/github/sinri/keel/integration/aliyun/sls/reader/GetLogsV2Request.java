package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.json.JsonObjectConvertible;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Request parameters for GetLogsV2 API.
 * <p>
 * This class encapsulates all parameters needed to query log data from a Logstore.
 *
 * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">GetLogsV2 -
 *         查询Logstore中的日志数据</a>
 * @since 5.0.0
 */
@NullMarked
public class GetLogsV2Request implements JsonObjectConvertible {
    /**
     * 查询开始时间点。Unix 时间戳格式，表示从 1970-1-1 00:00:00 UTC 计算起的秒数。
     * <p>
     * 请求参数 from 和 to 定义的时间区间遵循左闭右开原则，即该时间区间包括区间开始时间点，但不包括区间结束时间点。
     */
    private final int from;

    /**
     * 查询结束时间点。Unix 时间戳格式，表示从 1970-1-1 00:00:00 UTC 计算起的秒数。
     * <p>
     * 请求参数 from 和 to 定义的时间区间遵循左闭右开原则，即该时间区间包括区间开始时间点，但不包括区间结束时间点。
     */
    private final int to;

    /**
     * 仅当 query 参数为查询语句时，该参数有效，表示请求返回的最大日志条数。
     * <p>
     * 最小值为 0，最大值为 100，默认值为 100。
     */
    private final @Nullable Integer line;

    /**
     * 仅当 query 参数为查询语句时，该参数有效，表示查询开始行。
     * <p>
     * 默认值为 0。
     */
    private final @Nullable Integer offset;

    /**
     * 用于指定返回结果是否按日志时间戳降序返回日志，精确到分钟级别。
     * <p>
     * true：按照日志时间戳降序返回日志。
     * <p>
     * false（默认值）：按照日志时间戳升序返回日志。
     * <p>
     * 注意：当 query 参数为查询语句时，参数 reverse 有效，用于指定返回日志排序方式。
     * 当 query 参数为查询和分析语句时，参数 reverse 无效，由 SQL 分析语句中 order by 语法指定排序方式。
     */
    private final @Nullable Boolean reverse;

    /**
     * 是否开启增强 sql，默认关闭。
     */
    private final @Nullable Boolean powerSql;

    /**
     * 查询参数。
     * <p>
     * 例如：mode=scan
     */
    private final @Nullable String session;

    /**
     * 日志主题。默认值为双引号（""）。
     */
    private final @Nullable String topic;

    /**
     * 查询语句或者分析语句。
     * <p>
     * 在 query 参数的分析语句中加上 set session parallel_sql=true;，表示使用 SQL 独享版。
     * <p>
     * 当 query 参数中有分析语句（SQL 语句）时，该接口的 line 参数和 offset 参数无效，
     * 建议设置为 0，需通过 SQL 语句的 LIMIT 语法实现翻页。
     *
     * @see <a href="https://help.aliyun.com/zh/sls/log-search-overview">查询概述</a>
     * @see <a href="https://help.aliyun.com/zh/sls/log-analysis-overview">分析概述</a>
     */
    private final @Nullable String query;

    /**
     * scan 或短语查询表示是否向前或向后翻页。
     */
    private final @Nullable Boolean forward;

    /**
     * 是否高亮。
     */
    private final @Nullable Boolean highlight;

    private GetLogsV2Request(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.line = builder.line;
        this.offset = builder.offset;
        this.reverse = builder.reverse;
        this.powerSql = builder.powerSql;
        this.session = builder.session;
        this.topic = builder.topic;
        this.query = builder.query;
        this.forward = builder.forward;
        this.highlight = builder.highlight;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public @Nullable Integer getLine() {
        return line;
    }

    public @Nullable Integer getOffset() {
        return offset;
    }

    public @Nullable Boolean getReverse() {
        return reverse;
    }

    public @Nullable Boolean getPowerSql() {
        return powerSql;
    }

    public @Nullable String getSession() {
        return session;
    }

    public @Nullable String getTopic() {
        return topic;
    }

    public @Nullable String getQuery() {
        return query;
    }

    public @Nullable Boolean getForward() {
        return forward;
    }

    public @Nullable Boolean getHighlight() {
        return highlight;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put("from", from);
        json.put("to", to);
        if (line != null) {
            json.put("line", line);
        }
        if (offset != null) {
            json.put("offset", offset);
        }
        if (reverse != null) {
            json.put("reverse", reverse);
        }
        if (powerSql != null) {
            json.put("powerSql", powerSql);
        }
        if (session != null) {
            json.put("session", session);
        }
        if (topic != null) {
            json.put("topic", topic);
        }
        if (query != null) {
            json.put("query", query);
        }
        if (forward != null) {
            json.put("forward", forward);
        }
        if (highlight != null) {
            json.put("highlight", highlight);
        }
        return json;
    }

    @Override
    public final String toJsonExpression() {
        return toJsonObject().encode();
    }

    @Override
    public final String toFormattedJsonExpression() {
        return toJsonObject().encodePrettily();
    }

    /**
     * Builder for GetLogsV2Request.
     */
    public static class Builder {
        private int from;
        private int to;
        private @Nullable Integer line;
        private @Nullable Integer offset;
        private @Nullable Boolean reverse;
        private @Nullable Boolean powerSql;
        private @Nullable String session;
        private @Nullable String topic;
        private @Nullable String query;
        private @Nullable Boolean forward;
        private @Nullable Boolean highlight;

        private Builder() {
        }

        /**
         * Set the start time for the query (required).
         *
         * @param from Unix timestamp in seconds
         * @return this builder
         */
        public Builder from(int from) {
            this.from = from;
            return this;
        }

        /**
         * Set the end time for the query (required).
         *
         * @param to Unix timestamp in seconds
         * @return this builder
         */
        public Builder to(int to) {
            this.to = to;
            return this;
        }

        /**
         * Set the maximum number of log entries to return.
         *
         * @param line Maximum number of logs (0-100, default 100)
         * @return this builder
         */
        public Builder line(@Nullable Integer line) {
            this.line = line;
            return this;
        }

        /**
         * Set the offset for pagination.
         *
         * @param offset Starting row offset (default 0)
         * @return this builder
         */
        public Builder offset(@Nullable Integer offset) {
            this.offset = offset;
            return this;
        }

        /**
         * Set whether to return logs in descending order by timestamp.
         *
         * @param reverse true for descending, false for ascending (default)
         * @return this builder
         */
        public Builder reverse(@Nullable Boolean reverse) {
            this.reverse = reverse;
            return this;
        }

        /**
         * Set whether to enable power SQL.
         *
         * @param powerSql true to enable, false to disable (default)
         * @return this builder
         */
        public Builder powerSql(@Nullable Boolean powerSql) {
            this.powerSql = powerSql;
            return this;
        }

        /**
         * Set the session parameter.
         *
         * @param session Session parameter (e.g., "mode=scan")
         * @return this builder
         */
        public Builder session(@Nullable String session) {
            this.session = session;
            return this;
        }

        /**
         * Set the topic filter.
         *
         * @param topic Log topic (default "")
         * @return this builder
         */
        public Builder topic(@Nullable String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * Set the query or analysis statement.
         *
         * @param query Query or SQL statement
         * @return this builder
         */
        public Builder query(@Nullable String query) {
            this.query = query;
            return this;
        }

        /**
         * Set whether to page forward or backward in scan mode.
         *
         * @param forward true for forward, false for backward
         * @return this builder
         */
        public Builder forward(@Nullable Boolean forward) {
            this.forward = forward;
            return this;
        }

        /**
         * Set whether to highlight search terms.
         *
         * @param highlight true to highlight, false otherwise (default)
         * @return this builder
         */
        public Builder highlight(@Nullable Boolean highlight) {
            this.highlight = highlight;
            return this;
        }

        /**
         * Build the GetLogsV2Request instance.
         *
         * @return the constructed request
         */
        public GetLogsV2Request build() {
            return new GetLogsV2Request(this);
        }
    }
}
