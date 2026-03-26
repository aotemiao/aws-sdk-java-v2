# SDK V2 RequestBody Content-Length 决策记录

## 记录条目模板

**来源：**（会议/随口讨论/结对编程/每日站会）用于（讨论/实现）X

**出席：** Anna-Karin、Dongie、Nico、John、Zoe

**已关闭决策：**

1. 问题？决策。理由。

**开放决策：**

1. （旧有/重新打开/新增）问题？

## 2021-04-08

**来源：** 讨论在同步 `RequestBody` 中新增 `Optional<Long>` getter 后如何处理已弃用的 content-length getter：https://github.com/aws/aws-sdk-java-v2/blob/master/core/sdk-core/src/main/java/software/amazon/awssdk/core/sync/RequestBody.java#L66-L71

**出席：** Anna-Karin、Dongie、Nico、John、Zoe

**已关闭决策：**

1. 是否应用新的 optional getter 替换旧 content-length getter？否，属破坏性变更；添加 `@Deprecated` 更可接受。
2. 是否应用负值表示 null content-length？是，负值可与正常的零 content-length 区分。
3. 是否在已弃用 getter 中于 content-length 为负时抛异常？是，这样使用已弃用方法的客户会获知应改用 `Optional<Long>` getter；且此前 content-length 始终非负，此处抛异常不会破坏既有正确用法。
   
**开放决策：**

无
