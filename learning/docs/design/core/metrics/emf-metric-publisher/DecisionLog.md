# AWS SDK for Java v2 EMFMetricPublisher 决策记录


## 2024-11-19

**来源：** 团队设计文档评审会议，议题为 CloudWatchMetricPublisher 在 Lambda 中表现不佳

**出席：** Bole、David、Debora、Olivier、Dongie、Zoe

**已关闭决策：**

1. 是否应在其中支持 CloudWatch 客户端？否，写入日志即可；若需要 CW 客户端则 `CloudWatchMetricPublisher` 足够。
2. 是否应创建 logger 还是由客户传入 logger 实例？否，短期内避免由客户传入 logger 以防配置错误。应探索用例并试用；传入日志级别可能是好主意。默认使用 `Logger.info` 级别。
3. 在 convert 方法中是否使用 Jackson（更易但较慢）（双向门）？是，convert 方法可使用第三方 jackson-core，属双向门，后续可实现其他方式并做性能测试。
4. 是否改用 SDK 的 `LoggingMetricPublisher`？否，致命问题是其位于 `core/metric-spi`，应将模块与之分离。
5. 是否改用 `MetricLogger`？否，其依赖 jackson-databind，易受 CVE 影响。

**开放决策：**

无
