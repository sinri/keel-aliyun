---
layout: default
title: Keel-Aliyun 5.0.3 用户指南
---

# Keel-Aliyun 5.0.3 用户指南

Keel-Aliyun 是 [Keel](https://github.com/sinri/Keel) 框架的阿里云集成库，基于 Vert.x 5.x 异步模型，提供以下能力：

- **SLS 日志写入** — 将应用日志异步批量写入阿里云日志服务（SLS）
- **SLS 日志查询** — 通过 GetLogsV2 API 查询 SLS 中的日志数据
- **SLS 时序指标** — 向 SLS MetricStore 写入时序监控指标
- **SAE 任务集成** — 阿里云 Serverless 应用引擎（SAE）定时任务模板

## 环境要求

| 依赖            | 版本     |
|---------------|--------|
| Java          | 17+    |
| Vert.x        | 5.1.3+ |
| Keel Core     | 5.0.3+ |
| LZ4 Java      | 1.11.1 |
| Protobuf Java | 4.31.1 |

## 5.0.3 更新摘要

- 升级 Keel Core 到 `5.0.3`，测试依赖升级到 `keel-test 5.0.4`。
- 升级 `at.yawk.lz4:lz4-java` 到 `1.11.1`，包含 `CVE-2026-59949` 的上游安全修复。
- SLS PutLogs 的 `Content-MD5` 头与签名串统一使用同一 payload 摘要，避免签名输入不一致。
- 明确 PutLogs 请求体使用 SLS 兼容的单个 `LogGroup` Protobuf body。
- SLS 写入失败时增加 fallback 输出，会将原始 `LogGroup` 内容写入内部 logger，便于排查未成功上传的日志。
- `SlsMetricRecorder` 构造 `__labels__` 时不再原地修改调用方传入的 labels map。
- 优化 `SlsLoggerFactory` 停止流程，避免重复 undeploy SLS writer。

## 引入依赖

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("io.github.sinri:keel-aliyun:5.0.3")
}
```

### Gradle (Groovy DSL)

```groovy
dependencies {
    implementation 'io.github.sinri:keel-aliyun:5.0.3'
}
```

### Maven

```xml

<dependency>
    <groupId>io.github.sinri</groupId>
    <artifactId>keel-aliyun</artifactId>
    <version>5.0.3</version>
</dependency>
```

## Java Module System

本库使用 JPMS 模块化，模块名为 `io.github.sinri.keel.integration.aliyun`。

在你的 `module-info.java` 中添加：

```java
module your.module {
    requires io.github.sinri.keel.integration.aliyun;
}
```

## 快速开始

以下是一个最简单的 SLS 日志写入示例：

```java
// 1. 从配置中读取 SLS 配置
AliyunSlsConfigElement slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());

// 2. 创建日志工厂
SlsLoggerFactory loggerFactory = new SlsLoggerFactory(slsConfig);

// 3. 以 Worker Verticle 方式部署
loggerFactory.

deployMe(keel, new DeploymentOptions()
        .

setThreadingModel(ThreadingModel.WORKER))
        .

onSuccess(deploymentId ->{
// 4. 创建 Logger 并写入日志
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

## 文档目录

| 文档                             | 说明                    |
|--------------------------------|-----------------------|
| [配置参考](configuration.md)       | 所有配置项的完整说明            |
| [SLS 日志写入](sls-log-writing.md) | 日志工厂、Logger 的使用与部署    |
| [SLS 日志查询](sls-log-reading.md) | 通过 GetLogsV2 API 查询日志 |
| [SLS 时序指标](sls-metric.md)      | 向 MetricStore 写入时序指标  |
| [SAE 任务集成](sae.md)             | Serverless 应用引擎任务模板   |

## 架构概览

```
┌──────────────────────────────────────────────────────┐
│                   你的应用代码                         │
├───────────────┬──────────────┬───────────┬────────────┤
│ SlsLoggerFactory │  SlsReader   │SlsMetric- │ SAETaskUnit│
│  (日志写入)       │  (日志查询)   │ Recorder  │  (SAE任务)  │
├───────────────┴──────────────┴───────────┴────────────┤
│              AliyunSlsConfigElement (配置)              │
├───────────────────────────────────────────────────────┤
│   AliyunSLSLogPutter (HTTP Client + Protobuf + LZ4)   │
├───────────────────────────────────────────────────────┤
│          AliyunSlsSignatureKit (HMAC-SHA1 签名)         │
├───────────────────────────────────────────────────────┤
│              Vert.x 5.x (异步 I/O)                     │
└───────────────────────────────────────────────────────┘
```

日志写入链路：应用代码 → Logger → QueuedLogWriterAdapter（批量缓冲）→ AliyunSLSLogPutter（Protobuf 序列化 + LZ4 压缩 + HMAC-SHA1 签名）→ SLS PutLogs API。

## 许可证

[GPL v3.0](https://www.gnu.org/licenses/gpl-3.0.txt)
