# 配置参考

Keel-Aliyun 通过 Keel 框架的 `ConfigElement` 体系读取配置。配置文件通常为 `config.properties` 或等效的层级化配置源。

## SLS 日志服务配置

配置路径：`aliyun.sls`

通过 `AliyunSlsConfigElement.forSls(ConfigElement.root())` 读取。

| 配置项                          | 类型      | 必填 | 说明                                         |
|------------------------------|---------|----|--------------------------------------------|
| `aliyun.sls.project`         | String  | 是  | SLS Project 名称                             |
| `aliyun.sls.logstore`        | String  | 是  | SLS Logstore 名称                            |
| `aliyun.sls.endpoint`        | String  | 是  | SLS 服务接入点，如 `cn-hangzhou.log.aliyuncs.com` |
| `aliyun.sls.accessKeyId`     | String  | 是  | 阿里云 AccessKey ID                           |
| `aliyun.sls.accessKeySecret` | String  | 是  | 阿里云 AccessKey Secret                       |
| `aliyun.sls.source`          | String  | 否  | 日志来源标识，支持 `[IP]` 占位符自动替换为本机 IP             |
| `aliyun.sls.disabled`        | Boolean | 否  | 设为 `true` 可禁用 SLS，退回到标准输出。默认 `false`       |

### source 占位符

`source` 字段支持模板表达式：

- 留空或不配置：使用 SLS 服务端默认行为
- `[IP]`：自动替换为本机 IP 地址
- `app-[IP]`：替换其中的 `[IP]` 部分，如生成 `app-192.168.1.100`

## SLS MetricStore 配置

配置路径：`aliyun.sls_metric`

通过 `AliyunSlsConfigElement.forSlsMetric(ConfigElement.root())` 读取。

配置项与 SLS 日志服务配置完全相同，但指向的是 MetricStore 类型的 Logstore。

| 配置项                                 | 类型      | 必填 | 说明                      |
|-------------------------------------|---------|----|-------------------------|
| `aliyun.sls_metric.project`         | String  | 是  | SLS Project 名称          |
| `aliyun.sls_metric.logstore`        | String  | 是  | MetricStore 名称          |
| `aliyun.sls_metric.endpoint`        | String  | 是  | SLS 服务接入点               |
| `aliyun.sls_metric.accessKeyId`     | String  | 是  | 阿里云 AccessKey ID        |
| `aliyun.sls_metric.accessKeySecret` | String  | 是  | 阿里云 AccessKey Secret    |
| `aliyun.sls_metric.source`          | String  | 否  | 来源标识，支持 `[IP]` 占位符      |
| `aliyun.sls_metric.disabled`        | Boolean | 否  | 设为 `true` 禁用。默认 `false` |

## 配置示例

### config.properties

```properties
# SLS 日志配置
aliyun.sls.project=my-project
aliyun.sls.logstore=my-logstore
aliyun.sls.endpoint=cn-hangzhou.log.aliyuncs.com
aliyun.sls.accessKeyId=LTAI5t**************
aliyun.sls.accessKeySecret=HhFr**************
aliyun.sls.source=[IP]

# SLS 时序指标配置（可选）
aliyun.sls_metric.project=my-project
aliyun.sls_metric.logstore=my-metric-store
aliyun.sls_metric.endpoint=cn-hangzhou.log.aliyuncs.com
aliyun.sls_metric.accessKeyId=LTAI5t**************
aliyun.sls_metric.accessKeySecret=HhFr**************
aliyun.sls_metric.source=[IP]
```

## SLS 接入点列表

根据你的 SLS Project 所在地域选择对应的接入点：

| 地域       | 公网接入点                          |
|----------|--------------------------------|
| 华东 1（杭州） | `cn-hangzhou.log.aliyuncs.com` |
| 华东 2（上海） | `cn-shanghai.log.aliyuncs.com` |
| 华北 1（青岛） | `cn-qingdao.log.aliyuncs.com`  |
| 华北 2（北京） | `cn-beijing.log.aliyuncs.com`  |
| 华南 1（深圳） | `cn-shenzhen.log.aliyuncs.com` |
| 中国香港     | `cn-hongkong.log.aliyuncs.com` |

> 完整列表请参考 [阿里云 SLS 服务接入点](https://help.aliyun.com/zh/sls/developer-reference/api-sls-2020-12-30-endpoint)。
> 若应用部署在阿里云 VPC 内，建议使用内网接入点（`-intranet` 后缀）以减少延迟和流量费用。

## 禁用 SLS 与降级行为

当 `disabled` 设为 `true`，或配置缺失/不完整时，`SlsLoggerFactory` 会自动降级为
`FallbackQueuedLogWriter`，将日志输出到标准输出（stdout），确保应用不会因为 SLS 不可用而启动失败。

此时控制台会输出提示：

```
Aliyun SLS Disabled, use fallback
```
