# Keel-Aliyun

Keel-Aliyun 是 [Keel](https://github.com/sinri/Keel) 框架的阿里云集成库，基于 Java 17、JPMS 和 Vert.x 5.x，提供阿里云 SLS 日志写入、SLS 日志查询、SLS MetricStore 指标写入和 SAE 任务集成能力。

## Features

- SLS 日志写入：异步队列缓冲，批量写入阿里云日志服务。
- SLS 日志查询：封装 GetLogsV2 API，支持查询、SQL 分析和结果标准化。
- SLS 时序指标：将 `MetricRecord` 写入 SLS MetricStore。
- SAE 任务集成：提供 SAE 任务生命周期、日志和指标扩展点。
- JPMS 支持：模块名为 `io.github.sinri.keel.integration.aliyun`。

## Requirements

| Dependency    | Version |
|---------------|--------:|
| Java          |     17+ |
| Keel Core     |  5.0.3+ |
| Vert.x        |  5.1.3+ |
| LZ4 Java      |  1.11.1 |
| Protobuf Java |  4.31.1 |

## Installation

Gradle Kotlin DSL:

```kotlin
dependencies {
    implementation("io.github.sinri:keel-aliyun:5.0.3")
}
```

Gradle Groovy DSL:

```groovy
dependencies {
    implementation 'io.github.sinri:keel-aliyun:5.0.3'
}
```

Maven:

```xml

<dependency>
    <groupId>io.github.sinri</groupId>
    <artifactId>keel-aliyun</artifactId>
    <version>5.0.3</version>
</dependency>
```

JPMS:

```java
module your.module {
    requires io.github.sinri.keel.integration.aliyun;
}
```

## Quick Start

```java
AliyunSlsConfigElement slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());
SlsLoggerFactory loggerFactory = new SlsLoggerFactory(slsConfig);

loggerFactory.

deployMe(keel, new DeploymentOptions()
        .

setThreadingModel(ThreadingModel.WORKER))
        .

onSuccess(deploymentId ->{
Logger logger = loggerFactory.createLogger("MyApp");
        logger.

info("Application started");
        logger.

warning("Something needs attention");
        logger.

error(log ->log
        .

message("An error occurred")
            .

context("userId","12345")
            .

exception(someException));
        });
```

`SlsLoggerFactory` 是 Vert.x Verticle。请在部署成功后再创建 logger，并使用
`ThreadingModel.WORKER` 部署以避免日志批量写入影响 event loop。

## Configuration

SLS 日志配置路径为 `aliyun.sls`：

```properties
aliyun.sls.project=my-project
aliyun.sls.logstore=my-logstore
aliyun.sls.endpoint=cn-hangzhou.log.aliyuncs.com
aliyun.sls.accessKeyId=****
aliyun.sls.accessKeySecret=****
aliyun.sls.source=[IP]
```

SLS MetricStore 配置路径为 `aliyun.sls_metric`：

```properties
aliyun.sls_metric.project=my-project
aliyun.sls_metric.logstore=my-metric-store
aliyun.sls_metric.endpoint=cn-hangzhou.log.aliyuncs.com
aliyun.sls_metric.accessKeyId=**************
aliyun.sls_metric.accessKeySecret=**************
aliyun.sls_metric.source=[IP]
```

`source` 支持 `[IP]` 占位符。配置缺失或 `disabled=true` 时，SLS 日志工厂会降级到 stdout。

## Behavior Notes

- 日志写入会先序列化为 SLS 兼容的 `LogGroup` Protobuf body，再使用 LZ4 压缩并通过 HMAC-SHA1 签名发送。
- 5.0.3 起，`Content-MD5` 请求头和签名串使用同一个压缩后 payload 摘要。
- SLS HTTP 返回非 200 或网络发送失败时，写入器会通过内部 logger 输出 fallback 内容，包括失败原因和原始
  `LogGroup` 内容。该策略用于避免日志系统异常中断业务流程。
- `SlsMetricRecorder` 会复制调用方传入的 labels 后再注入 `source`，不会原地修改 `MetricRecord.labels()`。

## Documentation

- [5.0.3 用户指南](docs/5.0.3/)
- [配置参考](docs/5.0.3/configuration.md)
- [SLS 日志写入](docs/5.0.3/sls-log-writing.md)
- [SLS 日志查询](docs/5.0.3/sls-log-reading.md)
- [SLS 时序指标](docs/5.0.3/sls-metric.md)
- [SAE 任务集成](docs/5.0.3/sae.md)

## Build

```bash
./gradlew clean build
```

Useful checks:

```bash
./gradlew test
./gradlew dependencyInsight --dependency lz4-java --configuration runtimeClasspath
```

## 5.0.3 Highlights

- Upgraded `keel-core` to `5.0.3`.
- Upgraded `at.yawk.lz4:lz4-java` to `1.11.1`, including the upstream fix for `CVE-2026-59949`.
- Aligned SLS `Content-MD5` signing input.
- Confirmed PutLogs body serialization as a single SLS-compatible `LogGroup`.
- Added fallback logging for failed SLS writes.
- Avoided mutating metric labels passed by callers.
- Avoided duplicate SLS writer undeploy.

## License

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.txt)
