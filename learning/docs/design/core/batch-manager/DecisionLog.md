# SDK V2 Batch 工具决策记录

## 记录条目模板

**来源**：（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席**：Anna-Karin、Irene、Dongie、Matt、Vinod、John、Zoe、Debora、Bennett、Michael

**已关闭决策：**

1. 问题？决策。理由

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2021-07-15

**来源：** 快速会议，讨论 batch 工具命名

**出席：** Anna-Karin、Dongie、Matt、Vinod、John、Zoe、Debora、Bennett、Michael

**已关闭决策：** 

1. batching 工具应如何命名以提高可发现性与可理解性？命名为 `{Service}BatchManager`。该工具作用于底层服务客户端，不太像「utility」；同时也不替代底层客户端方法，故非 enhanced client。其行为与 TransferManager 类似，因此采用 BatchManager。

**开放决策：**

无

## 2021-07-13

**来源：** 跟进会议，讨论上次会议的开放决策及新问题

**出席：** Dongie、Matt、Zoe、Debora、Bennett、Michael

**已关闭决策：**

1. 是否尊重 `SendMessageRequest` 中的 `RequestOverrideConfiguration` 字段？是，将使用 `AwsRequestOverrideConfiguration` 的 `equals/hashCode` 分别批处理。
2. 如何在 `SendMessageRequest` 与 `SendMessageRequestBatchEntry` 之间映射？将通过名称匹配及某种定制配置（如 `batch.json`）生成映射。
3. 若 `SendMessageRequest` 中存在 `SendMessageRequestBatchEntry` 没有的字段或反之？讨论选项：*(a)* 接受 `SendMessageRequest`，若某字段在 `SendMessageRequestBatchEntry` 中无同名对应则抛异常；*(b)* 与 a 相同并增加 `(QueueUrl, SendMessageRequestBatchEntry)` 方法；*(c)* 仅接受 `(QueueUrl, SendMessageRequestBatchEntry)`；*(d)* 接受 `SendMessageRequest`，不匹配则构建失败，并调研（或将该字段列入排除白名单）。**决策：** 现阶段采用 (a)，若字段分歧扩大可转向 (b)。
4. 是否允许客户覆盖 `SendMessageRequest` → `SendMessageRequestBatchEntry` 的映射以改变默认行为或提高转换可见性？暂不允许，未来若字段分歧可能需要。
5. batch 工具是否仅使用每次只发一条请求的 `sendMessage()`，以保持请求与响应 1:1？现阶段是，但该方向欢迎客户反馈。
6. 是否应对 `sendMessages()` 接受流或迭代器？现阶段否，欢迎客户反馈。

**开放决策：**

1. （旧）batching 工具应如何命名以提高可发现性与可理解性？

## 2021-06-29

**来源：** 讨论 Batch 工具初始设计的会议：https://github.com/aws/aws-sdk-java-v2/pull/2563

**出席：** Dongie、Matt、Vinod、John、Zoe、Debora、Bennett、Michael

**已关闭决策：**

1. 批处理方法是做在客户端上还是如设计文档提议的独立工具中？独立工具。与 Waiters 等 API 一致。若要调整需整体调整各 API，宜一并进行。
2. 是否使用 `batch.json` 或 `batcher.json` 存储默认值与服务专属 batch 方法？是。不应把提供这些值的负担交给客户，且与 Waiters 等 API 一致；将考虑在 SDK 范围推广。
3. 是否为 `CompletableFuture<BatchResponse<SendMessageBatchResultEntry>>` 创建包装类？是。应创建如 `SQSBatchResponse` 的包装类，避免深层泛型嵌套，便于客户理解。
4. 是否包含手动 flush 及按缓冲区 flush 的选项？是。与 v1 对齐，并为客户提供额外能力。
5. batch 重试是否由客户端处理？是。与 SDK 其余部分一致由客户端重试；这些重试也可能被批量执行。
6. 若同步与异步接口看起来很相似，是否仍要分离？是。预期接口会随功能增加而分化，构建器也不同。
7. 异步客户端是否应在节流异常时改为逐条发送请求？否。这会削弱异步客户端意义。发送量仅受客户端最大连接数等限制；此外底层客户端已提供节流支持。

**开放决策：**

1. （新）batch 工具是否仅使用 `sendMessage()` 单条请求以保持 1:1？
   1. 若否，使用 `sendMessages()` 的客户如何将请求消息与响应消息关联？
2. （新）是否应对 `sendMessages()` 接受流或迭代器？
   1. 说明：该决策与开放问题 #1 相互制约——若确定 1:1，则不接受流/迭代器，反之亦然。
3. （新）batching 工具应如何命名以提高可发现性与可理解性？
