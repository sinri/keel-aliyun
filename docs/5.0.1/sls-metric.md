---
layout: default
title: SLS 时序指标
---

# SLS 时序指标

本模块提供基于阿里云 SLS MetricStore 的时序指标写入能力，适用于应用监控、业务指标采集等场景。

## 核心类

| 类                   | 说明                                                        |
|---------------------|-----------------------------------------------------------|
| `SlsMetricRecorder` | 时序指标记录器，继承 `AbstractMetricRecorder`，作为 Worker Verticle 部署 |

## 前提条件

在 SLS 控制台中需要创建 **MetricStore** 类型的 Logstore（而非普通 Logstore），并在 [配置](configuration.md) 中通过
`aliyun.sls_metric` 路径配置。

## 基本用法

### 1. 创建并部署 MetricRecorder

```java
AliyunSlsConfigElement metricConfig = AliyunSlsConfigElement.forSlsMetric(ConfigElement.root());
SlsMetricRecorder metricRecorder = new SlsMetricRecorder(metricConfig);

// 以 Worker Verticle 方式部署
metricRecorder.deployMe(keel, new DeploymentOptions()
        .setThreadingModel(ThreadingModel.WORKER))
    .onSuccess(id -> System.out.println("MetricRecorder deployed"))
    .onFailure(err -> System.err.println("Failed: " + err));
```

### 2. 写入指标

通过 `MetricRecord` 写入时序数据：

```java
// 记录 HTTP 请求计数
Map<String, String> labels = new HashMap<>();
labels.put("method", "GET");
labels.put("path", "/api/orders");
labels.put("status", "200");

MetricRecord record = new MetricRecord(
    "http_requests_total",      // 指标名称
    1.0,                         // 指标值
    labels,                      // 标签
    System.currentTimeMillis()   // 时间戳（毫秒）
);

metricRecorder.accept("http_metrics", record);
```

```java
// 记录请求延迟
Map<String, String> latencyLabels = new HashMap<>();
latencyLabels.put("method", "POST");
latencyLabels.put("path", "/api/orders");

MetricRecord latencyRecord = new MetricRecord(
    "http_request_duration_ms",
    235.5,
    latencyLabels,
    System.currentTimeMillis()
);

metricRecorder.accept("http_metrics", latencyRecord);
```

### 3. 关闭 MetricRecorder

```java
metricRecorder.undeployMe()
    .onSuccess(v -> System.out.println("MetricRecorder stopped"));
```

## MetricStore 字段映射

每条 `MetricRecord` 写入 SLS MetricStore 时，会被映射为以下字段：

| SLS 字段          | 说明                                                 |
|-----------------|----------------------------------------------------|
| `__name__`      | 指标名称，如 `http_requests_total`                       |
| `__value__`     | 指标值，double 类型                                      |
| `__time_nano__` | 纳秒级时间戳（毫秒时间戳 + `"000"` 后缀）                         |
| `__labels__`    | 标签序列化字符串，格式为 `key1#$#value1\|key2#$#value2`，按字典序排列 |

> `source` 标签会自动注入到 labels 中，值为配置中的 `source` 字段（经 `[IP]` 替换后）。

### Labels 格式示例

对于标签 `{method: "GET", path: "/api", source: "192.168.1.100"}`，生成的 `__labels__` 为：

```
method#$#GET|path#$#/api|source#$#192.168.1.100
```

## 与 SAETaskUnit 集成

`SAETaskUnit` 接口提供了 `getMetricRecorder()` 默认方法，可以在 SAE 任务中集成指标采集：

```java
public class MyTask implements SAETaskUnit {
    private SlsMetricRecorder metricRecorder;

    @Override
    public MetricRecorder getMetricRecorder() {
        return metricRecorder;
    }

    @Override
    public void launch(String[] args) {
        try {
            AliyunSlsConfigElement metricConfig =
                AliyunSlsConfigElement.forSlsMetric(ConfigElement.root());
            metricRecorder = new SlsMetricRecorder(metricConfig);
            // ... 部署并使用
        } catch (AliyunSLSDisabled e) {
            // MetricStore 未配置，跳过指标采集
        }
    }
}
```

## 注意事项

- MetricStore 与普通 Logstore 是不同的存储类型，在 SLS 控制台创建时需选择 MetricStore
- 指标数据的 `topic` 参数用于区分不同的指标组，建议按业务维度划分
- 与日志写入类似，指标写入也是异步批量的，不会逐条发送
- 当 `aliyun.sls_metric` 配置缺失或 `disabled=true` 时，构造函数会抛出 `AliyunSLSDisabled` 异常
