# v2 Transfer Manager 进度监听器决策记录

## 记录条目模板

**来源：**（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席：**（姓名）

**已关闭决策：**

1. 问题？决策。理由。

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2021-09-21

**来源：** 讨论为 [TransferManager v2](https://github.com/aws/aws-sdk-java-v2/tree/master/docs/design/services/s3/transfermanager) 实现进度监听器的会议

**出席：** Anna-Karin、Bennett、Dongie、John、Matt、Zoe

**已关闭决策：**

1. **`ExecutionListener` 是否应（在底层）实现为 `ExecutionInterceptor`？**
    1. 认为是，但要求 `ExecutionListener` 的 API 本质上是 `ExecutionInterceptor` 的子集，这可能构成限制。必要时可考虑向 `Interceptor` 增补方法以支持 `Listener`；也可考虑将部分指标实现迁移为基于 `Listener`。
1. **`TransferProgress` 应可变还是不可变？**
    1. 希望遵循 v2 不可变设计原则，但 `TransferProgress` 常用于轮询传输状态。一致意见为 `TransferProgress` 应不可变，可通过不可变对象的*快照*实现，例如 `TransferProgressSnapshot`。
1. **需要何种程度的 CRT 支持/集成？**
    1. 认为 CRT 的 [ResponseDataConsumer](https://github.com/awslabs/aws-crt-java/blob/main/src/main/java/s3NativeClient/com/amazonaws/s3/ResponseDataConsumer.java#L22)
    可能足以实现所提议的 `TransferListener` 接口，因此可将 `ExecutionListener` 的实现推迟到确有需要时。

**开放决策：**

1. **是否需要 `ExecutionMode` 所提议的能力？**
    1. 尚不明确。可能仅保留给已有专用线程池的异步客户端，并让客户自定义卸载语义更清晰；也需与异步 future 完成方式一致，其中 `Runnable::run` 可类比所提议的「INLINE」。在深入实现前暂缓该想法。
