---
layout: default
title: SLS 日志查询
---

# SLS 日志查询

本模块提供通过阿里云 SLS GetLogsV2 API 查询日志数据的能力。

> **Technical Preview**：本功能标记为技术预览（`@TechnicalPreview`），API 可能在后续版本中调整。

## 核心类

| 类 | 说明 |
|----|------|
| `SlsReader` | 日志查询客户端，封装 GetLogsV2 API 调用 |
| `GetLogsV2Request` | 查询请求参数，提供 Builder 模式构建 |
| `GetLogsV2Response` | 查询响应，包含 `Meta` 元信息和日志数据 |
| `SlsQueryResultRow` | 单条日志结果，提供字段解析和标准化方法 |

## 基本用法

### 1. 创建 SlsReader

```java
AliyunSlsConfigElement slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());
SlsReader slsReader = new SlsReader(keel, slsConfig);
```

### 2. 构建查询请求

使用 Builder 模式构建 `GetLogsV2Request`：

```java
// 查询最近 1 小时的日志
int now = Math.toIntExact(System.currentTimeMillis() / 1000);
int oneHourAgo = now - 3600;

GetLogsV2Request request = GetLogsV2Request.builder()
    .from(oneHourAgo)
    .to(now)
    .query("level: ERROR")  // SLS 查询语句
    .line(100)               // 最多返回 100 条
    .offset(0)               // 从第 0 条开始
    .reverse(true)           // 按时间倒序
    .build();
```

### 3. 执行查询

```java
slsReader.callGetLogsV2(request)
    .onSuccess(response -> {
        // 检查查询是否完整
        GetLogsV2Response.Meta meta = response.getMeta();
        System.out.println("Progress: " + meta.getProgress());
        System.out.println("Processed rows: " + meta.getProcessedRows());
        System.out.println("Elapsed: " + meta.getElapsedMillisecond() + "ms");
        System.out.println("Result count: " + meta.getCount());

        // 遍历查询结果
        List<SlsQueryResultRow> rows = response.getQueryResultRows();
        for (SlsQueryResultRow row : rows) {
            System.out.println(row.normalize().encodePrettily());
        }
    })
    .onFailure(err -> {
        System.err.println("Query failed: " + err.getMessage());
    });
```

### 4. 关闭 Reader

使用完毕后关闭以释放 WebClient 资源：

```java
slsReader.close()
    .onSuccess(v -> System.out.println("Reader closed"));
```

## GetLogsV2Request 参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `from` | int | 是 | 查询起始时间，Unix 时间戳（秒）。时间区间为左闭右开 `[from, to)` |
| `to` | int | 是 | 查询结束时间，Unix 时间戳（秒） |
| `query` | String | 否 | 查询语句或分析语句（SQL）。参考 [SLS 查询语法](https://help.aliyun.com/zh/sls/log-search-overview) |
| `line` | Integer | 否 | 返回最大日志条数，0-100，默认 100。仅在 `query` 为查询语句时有效 |
| `offset` | Integer | 否 | 查询起始行，默认 0。仅在 `query` 为查询语句时有效 |
| `reverse` | Boolean | 否 | `true` 按时间降序，`false`（默认）按时间升序。仅在 `query` 为查询语句时有效 |
| `topic` | String | 否 | 日志主题过滤，默认为空 |
| `powerSql` | Boolean | 否 | 是否启用增强 SQL（SQL 独享版） |
| `session` | String | 否 | 查询参数，如 `mode=scan` |
| `forward` | Boolean | 否 | scan/短语查询的翻页方向 |
| `highlight` | Boolean | 否 | 是否高亮搜索词 |

> 当 `query` 中包含分析语句（竖线后的 SQL 部分）时，`line` 和 `offset` 参数无效，需通过 SQL 的 `LIMIT` 语法控制翻页。

## GetLogsV2Response 结构

### Meta 元信息

| 字段 | 类型 | 说明 |
|------|------|------|
| `progress` | String | `"Complete"` 结果完整；`"Incomplete"` 需重复请求 |
| `count` | int | 本次返回的日志行数 |
| `processedRows` | int | 查询处理的总行数 |
| `processedBytes` | int | 查询处理的总字节数 |
| `elapsedMillisecond` | int | 查询耗时（毫秒） |
| `hasSQL` | boolean | 是否为 SQL 分析查询 |
| `whereQuery` | String | 查询语句中竖线之前的部分 |
| `aggQuery` | String | 查询语句中竖线之后的 SQL 部分 |
| `keys` | List\<String\> | 结果中包含的所有字段名 |
| `isAccurate` | boolean | 结果是否秒级精确 |
| `mode` | Integer | 查询模式：0=普通查询，1=短语查询，2=SCAN，3=SCAN SQL |
| `cpuSec` | Double | SQL 独享版消耗的 CPU 核时 |
| `cpuCores` | Integer | 使用的 CPU 核数 |
| `scanBytes` | Integer | SCAN 模式下扫描的字节数 |

### SlsQueryResultRow

每条日志结果包含以下预定义字段：

| 方法 | 说明 |
|------|------|
| `getTime()` | 日志时间戳（Unix 秒） |
| `getSource()` | 日志来源 |
| `getTopic()` | 日志 topic |
| `getTagMap()` | 解析 `__tag__:xxx` 前缀的标签为 Map |
| `normalize()` | 将原始数据标准化为结构化 JsonObject |

`normalize()` 返回的 JsonObject 结构：

```json
{
  "topic": "MyApp",
  "time": 1700000000,
  "source": "192.168.1.100",
  "tags": {
    "receive_time": "1700000001"
  },
  "properties": {
    "level": "INFO",
    "message": "Order created",
    "context": "{\"orderId\":\"ORD-001\"}"
  }
}
```

- `tags`：从 `__tag__:xxx` 格式的字段中解析得到
- `properties`：所有非 `__` 前缀的自定义字段

## 处理不完整结果

当 `meta.getProgress()` 返回 `"Incomplete"` 时，表示查询数据量较大，当前结果不完整。此时应重复发送相同请求直到获得完整结果：

```java
Future<GetLogsV2Response> queryUntilComplete(SlsReader reader, GetLogsV2Request request) {
    return reader.callGetLogsV2(request)
        .compose(response -> {
            if (response.getMeta().isCompleted()) {
                return Future.succeededFuture(response);
            }
            // 结果不完整，等待后重试
            return keel.asyncSleep(500)
                .compose(v -> queryUntilComplete(reader, request));
        });
}
```

## 使用 SQL 分析

```java
// SQL 分析查询 - 统计各级别日志数量
GetLogsV2Request request = GetLogsV2Request.builder()
    .from(oneHourAgo)
    .to(now)
    .query("* | SELECT level, COUNT(*) AS cnt GROUP BY level ORDER BY cnt DESC LIMIT 100")
    .build();

slsReader.callGetLogsV2(request)
    .onSuccess(response -> {
        response.getData().forEach(row -> {
            System.out.println(row.getString("level") + ": " + row.getInteger("cnt"));
        });
    });
```

> SQL 分析时请将 `line` 和 `offset` 设为默认值（不设置），翻页通过 SQL 的 `LIMIT/OFFSET` 控制。

## 完整示例

```java
public Future<Void> queryRecentErrors(Keel keel) {
    AliyunSlsConfigElement config;
    try {
        config = AliyunSlsConfigElement.forSls(ConfigElement.root());
    } catch (NotConfiguredException e) {
        return Future.failedFuture(e);
    }

    SlsReader reader = new SlsReader(keel, config);
    int now = Math.toIntExact(System.currentTimeMillis() / 1000);

    GetLogsV2Request request = GetLogsV2Request.builder()
        .from(now - 3600)
        .to(now)
        .query("level: ERROR")
        .line(50)
        .reverse(true)
        .build();

    return reader.callGetLogsV2(request)
        .onSuccess(response -> {
            System.out.println("Found " + response.getMeta().getCount() + " errors");
            response.getQueryResultRows().forEach(row -> {
                JsonObject normalized = row.normalize();
                System.out.println(normalized.encodePrettily());
            });
        })
        .eventually(reader::close)
        .mapEmpty();
}
```
