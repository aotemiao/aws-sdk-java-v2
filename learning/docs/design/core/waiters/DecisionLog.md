# SDK V2 Waiters 决策记录

说明：决策记录流程在本项目中启用较晚，因此 2020 年 9 月 22 日之前的决策未列入下文。

## 记录条目模板

**来源：**（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席：** Anna-Karin、Ben、Dongie、Irene、Matt、Nico、Vinod、John、Zoe

**已关闭决策：**

1. 问题？决策。理由。

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2020-09-22

**来源：** 评审 waiters 实现 API 面宽的会议 https://github.com/aws/aws-sdk-java-v2/tree/waiters-development

**出席：** Anna-Karin、Ben、Dongie、John、Matt、Nico、Vinod、Zoe

**已关闭决策：**

1. 是否应修改默认 `ScheduledExecutorService` 核心线程数？是，应改为 1，因其仅用于调度重试，5 显得过多。
2. 是否应放宽 `DynamodbEnhancedClient#Builder` 的校验，并将 `DynamodbEnhancedClient.builder().build()` 更新为创建默认 SDK 客户端？是，因为 `DynamodbEnhancedClient.builder().build` 应与 `DynamodbEnhancedClient.create()` 等价。
3. 是否应将泛型 `Waiter` 设为非公开？否，它本质上是受保护 API，且无论如何需保持向后兼容；此外，在服务 waiter 不可用时客户仍可受益。
4. 是否应创建联合类型 `ResponseOrException`？是，因其明确表示响应与异常互斥、仅其一存在。应保持命名 `ResponseOrException`，直观且具描述性。
5. 是否应重命名 `WaiterResponse#responseOrExecption`？是，应改为 `WaiterResponse#matched`，因其相较 `matchedResponse`、`matchedResult`、`matchedValue` 等选项得票最高。
6. 是否应重命名 `{Service}Waiter.Builder#executorService`？是，应改为 `{Service}Waiter.Builder#scheduledExecutorService`，以更明确表示其为 `ScheduledExecutorService`。
7. 是否应重命名 `Waiter.Builder#pollingStrategy`？是，应改为 `Waiter.Builder#waiterOverrideConfiguration`，以便需要时更易扩展配置。`WaiterOverrideConfiguration` 下所有配置均应有默认值，与 `ClientOverrideConfiguration` 一致。
8. 是否应支持按请求覆盖 waiter 配置？是，这是合理功能；应为每个 waiter 操作提供重载方法，接受 `WaiterOverrideConfiguration` 参数，例如 `DynamodbWaiter#waitUntilTableExists(DescribeTableRequest, WaiterOverrideConfiguration)`。
   
**开放决策：**

无
