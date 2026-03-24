# SLS 日志写入

本模块提供基于阿里云日志服务（SLS）的异步日志写入能力。日志通过队列缓冲、Protobuf 序列化、LZ4 压缩后批量上传至 SLS。

## 核心类

| 类                        | 说明                                                    |
|--------------------------|-------------------------------------------------------|
| `SlsLoggerFactory`       | 日志工厂，实现 Keel `LoggerFactory` 接口，作为 Worker Verticle 部署 |
| `AliyunSlsConfigElement` | SLS 配置读取，详见 [配置参考](configuration.md)                  |

## 基本用法

### 1. 创建并部署日志工厂

`SlsLoggerFactory` 是一个 Vert.x Verticle，使用前必须部署：

```java
// 读取配置
AliyunSlsConfigElement slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());

// 创建工厂
SlsLoggerFactory loggerFactory = new SlsLoggerFactory(slsConfig);

// 以 Worker Verticle 方式部署（推荐）
loggerFactory.deployMe(keel, new DeploymentOptions()
        .setThreadingModel(ThreadingModel.WORKER))
    .onSuccess(deploymentId -> {
        System.out.println("SLS Logger Factory deployed: " + deploymentId);
    })
    .onFailure(err -> {
        System.err.println("Failed to deploy: " + err.getMessage());
    });
```

> **重要**：必须使用 `ThreadingModel.WORKER` 部署，否则日志批量写入的阻塞操作可能影响 event loop。

### 2. 创建 Logger 并写日志

部署完成后，通过工厂创建 Logger 实例：

```java
// 创建普通 Logger，topic 用于区分日志来源
Logger logger = loggerFactory.createLogger("OrderService");

// 基本日志
logger.info("Order created successfully");
logger.warning("Inventory is running low");
logger.error("Payment processing failed");

// 带上下文的日志
logger.info(log -> log
    .message("Order processed")
    .context("orderId", "ORD-2024-001")
    .context("amount", 99.99)
    .context("userId", "U12345"));

// 带异常信息的日志
try {
    // ... 业务逻辑
} catch (Exception e) {
    logger.error(log -> log
        .message("Failed to process order")
        .context("orderId", orderId)
        .exception(e));
}
```

### 3. 使用 SpecificLogger（类型化日志）

对于需要结构化日志字段的场景，可以使用 `SpecificLogger`：

```java
// 定义日志结构
public class OrderLog extends SpecificLog<OrderLog> {
    public OrderLog orderId(String orderId) {
        extra().put("orderId", orderId);
        return this;
    }

    public OrderLog amount(double amount) {
        extra().put("amount", amount);
        return this;
    }
}

// 创建类型化 Logger
SpecificLogger<OrderLog> orderLogger = loggerFactory.createLogger("OrderService", OrderLog::new);

// 使用类型化字段
orderLogger.info(log -> log
    .message("Order completed")
    .orderId("ORD-001")
    .amount(199.99));
```

### 4. 关闭日志工厂

应用关闭时，取消部署工厂以确保缓冲区中的日志被刷出：

```java
loggerFactory.undeployMe()
    .onSuccess(v -> System.out.println("Logger factory stopped"))
    .onFailure(err -> System.err.println("Error stopping: " + err));
```

## 日志字段映射

写入 SLS 时，每条日志会被映射为以下字段：

| SLS 字段           | 来源                             | 说明                               |
|------------------|--------------------------------|----------------------------------|
| `level`          | `SpecificLog.level()`          | 日志级别（INFO, WARNING, ERROR 等）     |
| `message`        | `SpecificLog.message()`        | 日志消息                             |
| `classification` | `SpecificLog.classification()` | 日志分类，JSON 数组格式                   |
| `exception`      | `SpecificLog.exception()`      | 异常堆栈，JSON 格式                     |
| `context`        | `SpecificLog.context()`        | 上下文数据，JSON 对象格式                  |
| *(自定义字段)*        | `SpecificLog.extra()`          | extra 中的每个 key-value 直接作为 SLS 字段 |

## 写入机制

### 批量缓冲

日志不会逐条发送，而是通过 `QueuedLogWriterAdapter` 批量处理：

- 默认缓冲区大小为 **128 条**（可在构造时自定义）
- 同一 topic 的日志会被聚合到一个 `LogGroup` 中
- 当单个 `LogGroup` 的预估大小超过 **5MB** 时，自动分片发送

### 传输协议

1. 日志数据序列化为 **Protobuf** 格式（proto2，与 SLS API 兼容）
2. Protobuf 数据经 **LZ4 压缩**
3. 使用 **HMAC-SHA1** 签名认证
4. 通过 Vert.x `WebClient` 异步发送 HTTPS 请求至 SLS PutLogs API

### 降级策略

当 SLS 配置缺失或 `disabled=true` 时，日志工厂自动切换到 `FallbackQueuedLogWriter`，将日志输出到标准输出（stdout），保证应用不因日志系统异常而中断。

## 完整示例

```java
public class MyApplication extends KeelVerticleBase {

    private SlsLoggerFactory loggerFactory;

    @Override
    protected Future<Void> startVerticle() {
        AliyunSlsConfigElement slsConfig;
        try {
            slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());
        } catch (NotConfiguredException e) {
            return Future.failedFuture(e);
        }

        loggerFactory = new SlsLoggerFactory(slsConfig);
        return loggerFactory.deployMe(getKeel(),
                new DeploymentOptions().setThreadingModel(ThreadingModel.WORKER))
            .compose(id -> {
                Logger logger = loggerFactory.createLogger("MyApp");
                logger.info("Application started");
                return Future.succeededFuture();
            });
    }

    @Override
    protected Future<Void> stopVerticle() {
        if (loggerFactory != null) {
            return loggerFactory.undeployMe();
        }
        return Future.succeededFuture();
    }
}
```

## 注意事项

- `SlsLoggerFactory` 本身也是 Verticle，在 `deployMe` 返回成功之前不要调用 `createLogger`
- 日志写入是异步的，`logger.info(...)` 调用后日志不会立即发送，而是进入缓冲队列
- SLS PutLogs API 单次请求限制为 5MB，本库会自动分片处理超大批次
- 写入失败时不会抛出异常（避免影响业务），错误信息会通过内部 logger 输出到 stdout
