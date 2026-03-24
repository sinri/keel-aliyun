---
layout: default
title: SAE 任务集成
---

# SAE 任务集成

本模块提供阿里云 Serverless 应用引擎（SAE）定时任务模板的基础抽象，帮助规范化 SAE 任务的生命周期管理。

## 核心类

| 类                      | 说明                          |
|------------------------|-----------------------------|
| `SAETaskUnit`          | 任务执行接口，定义任务启动、异常处理及日志/指标集成点 |
| `SAETaskUnitEnvReader` | SAE 运行时环境变量读取工具             |

## SAETaskUnit 接口

`SAETaskUnit` 定义了 SAE 任务的标准结构：

```java
public interface SAETaskUnit {
    // 启动任务
    void launch(String[] args);

    // 处理运行时异常
    void handleError(Throwable throwable);

    // 获取环境变量读取器
    SAETaskUnitEnvReader getEnvReader();

    // 获取日志工厂（默认为 stdout）
    default LoggerFactory getLoggerFactory();

    // 获取指标记录器（默认为 null）
    default @Nullable MetricRecorder getMetricRecorder();
}
```

## 基本用法

### 实现任务类

```java
public class DailyReportTask implements SAETaskUnit {
    private final SAETaskUnitEnvReader envReader = new SAETaskUnitEnvReader();
    private SlsLoggerFactory loggerFactory;

    @Override
    public SAETaskUnitEnvReader getEnvReader() {
        return envReader;
    }

    @Override
    public LoggerFactory getLoggerFactory() {
        if (loggerFactory != null) {
            return loggerFactory;
        }
        return SAETaskUnit.super.getLoggerFactory(); // 默认 stdout
    }

    @Override
    public void launch(String[] args) {
        try {
            String instanceId = envReader.getInstanceId();
            System.out.println("Task running on instance: " + instanceId);

            // 可选：初始化 SLS 日志
            try {
                AliyunSlsConfigElement slsConfig =
                    AliyunSlsConfigElement.forSls(ConfigElement.root());
                loggerFactory = new SlsLoggerFactory(slsConfig);
                // 部署并使用...
            } catch (Exception e) {
                System.out.println("SLS not available, using stdout");
            }

            // 执行业务逻辑
            generateDailyReport(args);

        } catch (Throwable t) {
            handleError(t);
        }
    }

    @Override
    public void handleError(Throwable throwable) {
        System.err.println("Task failed: " + throwable.getMessage());
        throwable.printStackTrace();
        System.exit(1);
    }

    private void generateDailyReport(String[] args) {
        // 业务逻辑...
    }

    static void main(String[] args) {
        new DailyReportTask().launch(args);
    }
}
```

## SAETaskUnitEnvReader

读取 SAE 运行时注入的环境变量：

| 方法                | 环境变量          | 说明                                      |
|-------------------|---------------|-----------------------------------------|
| `getInstanceId()` | `INSTANCE_ID` | 当前实例 ID。如果未设置会抛出 `NullPointerException` |

> SAE 任务在执行时会自动注入 `INSTANCE_ID` 等环境变量，可用于日志关联和分布式任务追踪。

## 与 SLS 日志/指标集成

`SAETaskUnit` 提供了 `getLoggerFactory()` 和 `getMetricRecorder()` 两个扩展点，支持在 SAE 任务中无缝集成 SLS：

```java
public class MonitoredTask implements SAETaskUnit {
    private SlsLoggerFactory loggerFactory;
    private SlsMetricRecorder metricRecorder;
    private final SAETaskUnitEnvReader envReader = new SAETaskUnitEnvReader();

    @Override
    public LoggerFactory getLoggerFactory() {
        return loggerFactory != null ? loggerFactory : SAETaskUnit.super.getLoggerFactory();
    }

    @Override
    public MetricRecorder getMetricRecorder() {
        return metricRecorder;
    }

    @Override
    public SAETaskUnitEnvReader getEnvReader() {
        return envReader;
    }

    @Override
    public void launch(String[] args) {
        try {
            // 初始化 SLS 日志
            AliyunSlsConfigElement slsConfig = AliyunSlsConfigElement.forSls(ConfigElement.root());
            loggerFactory = new SlsLoggerFactory(slsConfig);

            // 初始化 SLS 指标（可选）
            AliyunSlsConfigElement metricConfig = AliyunSlsConfigElement.forSlsMetric(ConfigElement.root());
            metricRecorder = new SlsMetricRecorder(metricConfig);

            // 业务逻辑...
            Logger logger = getLoggerFactory().createLogger("MonitoredTask");
            logger.info("Task started on instance " + envReader.getInstanceId());

        } catch (AliyunSLSDisabled e) {
            System.out.println("SLS disabled, continuing with stdout logging");
        } catch (Throwable t) {
            handleError(t);
        }
    }

    @Override
    public void handleError(Throwable throwable) {
        System.err.println("Task failed: " + throwable.getMessage());
        throwable.printStackTrace();
    }
}
```

## 注意事项

- `SAETaskUnit.launch()` 是同步方法，如果需要异步操作，应在内部管理 Vert.x 事件循环
- `getInstanceId()` 在非 SAE 环境中会因为环境变量缺失而抛出异常，本地开发时需设置 `INSTANCE_ID` 环境变量
- 默认的 `getLoggerFactory()` 返回 `StdoutLoggerFactory`，适合不需要 SLS 的轻量任务
- 默认的 `getMetricRecorder()` 返回 `null`，表示不采集指标
