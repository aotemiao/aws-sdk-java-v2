# Smithy Reference Architecture 身份与认证支持 — 决策记录

## 记录条目模板

**来源：**（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席：**（姓名）

**已关闭决策：**

1. 问题？决策。理由。

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2023-03-31

**来源：** SRA 相关 Identity 变更的 API 面评审会议

**出席：** Anna-Karin、David、Debora、Dongie、Jay、John、Matt、Olivier、Zoe

**已关闭决策：**

1. **新接口 `AwsCredentialsIdentity` 是否应提供 `create()` 方法？**
   1. 是，为客户提供便捷创建方式，无需依赖 `auth` 模块中的 `AwsBasicCredentials`。允许与 `AwsBasicCredentials` 有少量重复实现。
   2. `create()` 的实现可使用匿名内部类，而非再新增一个具名类。
2. **`AwsCredentialsProviderChain` 如何支持新 Identity 类型 `AwsCredentialsIdentity`？**
   1. `Builder.addCredentialsProvider()` 可重载以接受新类型。
   2. 可变参方法 `of()` 与 `Builder.credentialsProviders()` 可重载以接受新类型。零参调用不会产生歧义，因按 https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2.5 会选择更具体的方法，即 `AwsCredentialsProviderChain.of()` 会调用 `of(AwsCredentialsProvider...)`。
   3. 接受 `Collection` 的 `Builder.credentialsProviders()` 无法重载（擦除后签名相同）。应使用不同方法名 `credentialsIdentityProviders()`。我们不想在其他方法（可变参、`add`、`of`）名中一律加 `Identity`，以免被误解为链的另一类「属性」，因此仅在此一处使用单独方法名。
3. **`IdentityResolver` 如何声明其支持的 `IdentityProperty`？**
   1. `IdentityResolver` 应为每个支持的 `IdentityProperty` 定义 `public static` 字段，并文档说明在 `resolveIdentity` 中如何使用，以帮助调用方构造合适的 `ResolveIdentityRequest`。
   2. 是否需要对任意属性提供更强抽象？
      1. 讨论了指标收集器/遥测等潜在场景；若有强需求可后续补充，但需非 AWS 专属才能放入这些通用接口。

**开放决策：**

无
