# AWS CRT HTTP 客户端决策记录

说明：决策记录流程在本项目中启用较晚，因此 2020 年 8 月 24 日之前的决策未列入下文。

## 记录条目模板

**来源：**（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席：** Anna-Karin、Ben、Dongie、Irene、Matt、Nico、Vinod、John、Zoe

**已关闭决策：**

1. 问题？决策。理由。

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2020-08-24

**来源：** 评审 [AWS CRT HTTP Client](https://github.com/aws/aws-sdk-java-v2/tree/aws-crt-dev-preview/http-clients) API 面宽的会议

**出席：** Anna-Karin、Ben、Dongie、John、Matt、Nico、Vinod、Zoe

**已关闭决策：**

1. 应在所有 HTTP 客户端中增加静态工厂方法 `create`：便捷且一致。
2. 对外暴露的配置类应遵循 SDK 约定，与别处保持一致。
3. 应考虑重命名 `initialWindowSize`，以免与 HTTP/2 初始窗口混淆。可选名：`readBufferSize`。
4. 应考虑重命名 `httpMonitoringOptions`，因其不止做监控。可选名：`connectionHealthConfig`。
5. 应增加 service loader 类：与其他 HTTP 客户端一致；若晚加会破坏向后兼容。
6. 应测试并支持取消 HTTP 请求对应的 future：Netty Http Client 已支持。
7. 跟进预览后仍需支持的功能：
   1. 代理 TLS 信任库配置与 TLS 双向认证
   2. HTTP/2 支持
   3. 暴露连接池指标
   
**开放决策：**

无
