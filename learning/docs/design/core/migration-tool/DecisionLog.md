# AWS SDK for Java v2 迁移工具决策记录

## 记录条目模板

**来源**：（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席**：Anirudh、Anna-Karin、David、Dongie、Debora、Olivier、Matt、Jason、John、Zoe

**已关闭决策：**

1. 问题？决策。理由

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2024-01-26

**来源：** 每日站会与离线讨论：v2 迁移工具源码应托管在何处

**出席：** Anna-Karin、David、Debora、Olivier、Matt、Jason、John、Zoe

**已关闭决策：** 

1. 是否应将源码放在同一 aws-sdk-java-v2 仓库？是，因为：1）可作为 SDK 发布的一部分，无需额外发布设施；2）更易编写脚本生成配方，例如需脚本拉取各服务 service ID 与当前版本；3）可发现性更好。缺点是扩大仓库范围并略微增加构建与发布时间。替代方案：1）新建 GitHub 仓库 — 需新建并维护 CI/CD；2）仅内部托管 — 对客户体验差，代码不公开，用户无处提 issue/PR。

2. 是否应发布到 Maven Central，而非通过 S3 分发 JAR？是，因为绝大多数客户更愿意从包管理器消费库。

**开放决策：**

无
