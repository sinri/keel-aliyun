package io.github.sinri.keel.integration.aliyun.sls.reader;

import io.github.sinri.keel.base.json.UnmodifiableJsonifiableEntityImpl;
import io.vertx.core.json.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@NullMarked
public class GetLogsV2Response extends UnmodifiableJsonifiableEntityImpl {
    /**
     * 使用指定的 JSON 对象构造一个不可修改的 JSON 实体实例。
     * <p>
     * 构造函数会调用 {@link #purify(JsonObject)} 方法对输入的 JSON 对象进行净化处理。
     *
     * @param jsonObject 用于构造实体的非空 JSON 对象
     */
    public GetLogsV2Response(JsonObject jsonObject) {
        super(jsonObject);
    }

    public Meta getMeta() {
        return new Meta(readJsonObjectRequired("meta"));
    }

    public List<JsonObject> getData() {
        return readJsonObjectArrayRequired("data");
    }

    public List<SlsQueryResultRow> getQueryResultRows() {
        return getData().stream().map(SlsQueryResultRow::new).toList();
    }

    /**
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">返回参数</a>
     */
    @NullMarked
    public static class Meta extends UnmodifiableJsonifiableEntityImpl {

        /**
         * 使用指定的 JSON 对象构造一个不可修改的 JSON 实体实例。
         * <p>
         * 构造函数会调用 {@link #purify(JsonObject)} 方法对输入的 JSON 对象进行净化处理。
         *
         * @param jsonObject 用于构造实体的非空 JSON 对象
         */
        public Meta(JsonObject jsonObject) {
            super(jsonObject);
        }

        /**
         * 查询的结果是否完整。
         *
         * @return Complete：查询已经完成，返回结果为完整结果。Incomplete：查询已经完成，返回结果为不完整结果，需要重复请求以获得完整结果。
         */
        public String getProgress() {
            return readStringRequired("progress");
        }

        public boolean isCompleted() {
            return Objects.equals(getProgress(), "Complete");
        }

        /**
         * 查询语句中 {@code |} 之后的 SQL 部分。
         *
         * @return SQL 部分
         */
        public String getAggQuery() {
            return readStringRequired("aggQuery");
        }

        /**
         * 查询语句中 {@code |} 之前的部分。
         *
         * @return 查询部分
         */
        public String getWhereQuery() {
            return readStringRequired("whereQuery");
        }

        /**
         * 是否 SQL 查询。
         *
         * @return true 为 SQL 查询，false 为非 SQL 查询
         */
        public boolean getHasSQL() {
            return readBooleanRequired("hasSQL");
        }

        /**
         * 本次查询处理的行数。
         *
         * @return 处理的行数
         */
        public int getProcessedRows() {
            return readIntegerRequired("processedRows");
        }

        /**
         * 本次查询消耗的毫秒时间。
         *
         * @return 消耗的毫秒数
         */
        public int getElapsedMillisecond() {
            return readIntegerRequired("elapsedMillisecond");
        }

        /**
         * 独享 SQL 的核时。
         *
         * @return CPU 秒数
         */
        public @Nullable Double getCpuSec() {
            return readDouble("cpuSec");
        }

        /**
         * 使用 CPU 核数。
         *
         * @return CPU 核数
         */
        public @Nullable Integer getCpuCores() {
            return readInteger("cpuCores");
        }

        /**
         * 查询结果中所有的 key。
         *
         * @return key 列表
         */
        public List<String> getKeys() {
            return readStringArrayRequired("keys");
        }

        /**
         * 查询语句中所有的词。
         *
         * @return 词对象列表
         */
        public List<Term> getTerms() {
            return readJsonObjectArrayRequired("terms")
                    .stream()
                    .map(Term::new)
                    .toList();
        }

        /**
         * 限制条数，SQL 不带 limit 会返回。
         *
         * @return 限制的条数
         */
        public @Nullable Integer getLimited() {
            return readInteger("limited");
        }

        /**
         * 查询模式枚举。
         * <p>
         * 0: 普通查询（包括 SQL）
         * <p>
         * 1: 短语查询
         * <p>
         * 2: SCAN 扫描
         * <p>
         * 3: SCAN SQL
         *
         * @return 查询模式
         */
        public @Nullable Integer getMode() {
            return readInteger("mode");
        }

        /**
         * 短语查询信息。
         *
         * @return 短语查询信息对象
         */
        public @Nullable PhraseQueryInfo getPhraseQueryInfo() {
            JsonObject obj = readJsonObject("phraseQueryInfo");
            return obj == null ? null : new PhraseQueryInfo(obj);
        }

        /**
         * SCAN 时返回扫描的数据量（字节）。
         *
         * @return 扫描的字节数
         */
        public @Nullable Integer getScanBytes() {
            return readInteger("scanBytes");
        }

        //        /**
        //         * 高亮内容。
        //         *
        //         * @return 高亮内容数组
        //         */
        //        public @Nullable List<List<JsonObject>> getHighlights() {
        //            // highlights is an array of arrays, so we need special handling
        //            var highlightsArray = readJsonArray("highlights");
        //            if (highlightsArray == null) {
        //                return null;
        //            }
        //            return highlightsArray.stream()
        //                                  .map(item -> {
        //                                      if (item instanceof List) {
        //                                          return ((List<?>) item).stream()
        //                                                                 .filter(JsonObject.class::isInstance)
        //                                                                 .map(JsonObject.class::cast)
        //                                                                 .toList();
        //                                      }
        //                                      return List.<JsonObject>of();
        //                                  })
        //                                  .toList();
        //        }

        /**
         * 本次查询请求返回的日志行数。
         *
         * @return 日志行数
         */
        public int getCount() {
            return readIntegerRequired("count");
        }

        /**
         * 查询处理日志量（字节）。
         *
         * @return 处理的字节数
         */
        public int getProcessedBytes() {
            return readIntegerRequired("processedBytes");
        }

        /**
         * 是否秒级精确。
         *
         * @return true 为秒级精确，false 为非秒级精确
         */
        public boolean getIsAccurate() {
            return readBooleanRequired("isAccurate");
        }

        /**
         * 列类型。
         *
         * @return 列类型列表
         */
        public @Nullable List<String> getColumnTypes() {
            return readStringArray("columnTypes");
        }

        /**
         * 可观测数据类型。
         *
         * @return 数据类型
         */
        public @Nullable String getTelemetryType() {
            return readString("telemetryType");
        }
    }

    /**
     * 短语查询信息。
     *
     * @see <a href="https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-getlogsv2">返回参数</a>
     */
    @NullMarked
    public static class PhraseQueryInfo extends UnmodifiableJsonifiableEntityImpl {

        /**
         * 使用指定的 JSON 对象构造一个不可修改的 JSON 实体实例。
         * <p>
         * 构造函数会调用 {@link #purify(JsonObject)} 方法对输入的 JSON 对象进行净化处理。
         *
         * @param jsonObject 用于构造实体的非空 JSON 对象
         */
        public PhraseQueryInfo(JsonObject jsonObject) {
            super(jsonObject);
        }

        /**
         * 是否已经扫描了全部日志。
         *
         * @return true 表示已扫描全部，false 表示未扫描全部
         */
        public @Nullable Boolean getScanAll() {
            return readBoolean("scanAll");
        }

        /**
         * 本次扫描结果对应的索引过滤后的起始 offset。
         *
         * @return 起始 offset
         */
        public @Nullable Integer getBeginOffset() {
            return readInteger("beginOffset");
        }

        /**
         * 本次扫描结果对应的索引过滤后的结束 offset。
         *
         * @return 结束 offset
         */
        public @Nullable Integer getEndOffset() {
            return readInteger("endOffset");
        }

        /**
         * 本次扫描结果对应的索引过滤后的最后时间。
         *
         * @return 最后时间戳
         */
        public @Nullable Integer getEndTime() {
            return readInteger("endTime");
        }
    }

    @NullMarked
    public static class Term extends UnmodifiableJsonifiableEntityImpl {

        /**
         * 使用指定的 JSON 对象构造一个不可修改的 JSON 实体实例。
         * <p>
         * 构造函数会调用 {@link #purify(JsonObject)} 方法对输入的 JSON 对象进行净化处理。
         *
         * @param jsonObject 用于构造实体的非空 JSON 对象
         */
        public Term(JsonObject jsonObject) {
            super(jsonObject);
        }

        public String getTerm() {
            return readStringRequired("term");
        }

        public String getKey() {
            return readStringRequired("key");
        }
    }
}
