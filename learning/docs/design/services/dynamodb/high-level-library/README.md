**设计：** 新功能，**状态：** [已发布](../../../../../services-custom/dynamodb-enhanced/README.md)

## 信条（除非你有更好的）

1. 在客户问题空间中与其对接，使其能快速交付价值。
2. 满足客户预期驱动易用性。
3. 可发现性驱动使用量。
4. 提供面向 Java 的 DynamoDB 体验，降低接入 DynamoDB 的编码量。
5. 复用与生成式 DynamoDB 客户端相同的名词与动词，符合客户预期。
6. 优化冷启动性能，使客户在 Lambda 等环境中便于使用对象映射。

## 问题

AWS SDK for Java 2.x 的客户目前使用 `DynamoDbClient` 与 DynamoDB 通信。该客户端由 DynamoDB 团队提供的模型生成。

由于是生成客户端，其 Java 体验不够地道。例如：(1) 数字用 `String` 表示而非更地道的 `Number`；(2) 客户需手动将 `Instant` 等常见 Java 类型转为 DynamoDB 支持的类型；(3) 用 Java 对象表示 DynamoDB 数据的客户需手动将对象转为 DynamoDB 支持的项表示。

## 既有方案

目前已知没有第三方工具在 AWS SDK for Java 2.x 中直接解决该问题。在 1.11.x 中存在多种方案，包括 AWS 自带的 Document 与 Mapper 客户端。

## 提议方案 [已实现]

AWS SDK for Java 将新增「enhanced DynamoDB client」，作为生成式 DynamoDB API 数据访问部分的替代。控制面能力有限，主要为 `createTable`。

该 enhanced 客户端通过以下方式让 Java 客户更易使用 DynamoDB：
1. 支持 Java 对象与 DynamoDB 项之间的转换
2. 直接支持 DynamoDB 全部数据面操作
3. 使用与 DynamoDB 相同的动词与名词
4. 支持测试场景，例如用与数据面操作相同的模型建表

该库已有完整公开预览。见 [DynamoDb Enhanced Public Preview Library](../../../../../services-custom/dynamodb-enhanced/README.md)。
   
## 附录 A：需求功能

* [Immutable classes](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-315049138)
* [Getter/setter-less fields](https://github.com/aws/aws-sdk-java/issues/547)
* [Replace `PaginatedList` with `Stream`](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-318051305)
* [Allow 'setters' and 'getters' to support different types](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-318792534)
* [Have 'scan' respect the table's read throughput](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-329007523)
* [Allow creating a table with an LSI that projects all attributes](https://github.com/aws/aws-sdk-java/issues/214#issue-31304615)
* [Projection expressions in 'load' and 'batchLoad'](https://github.com/aws/aws-sdk-java/issues/527)
* [New condition expressions](https://github.com/aws/aws-sdk-java/issues/534)
* [Accessing un-modeled/dynamic attributes in a POJO](https://github.com/aws/aws-sdk-java/issues/674)
* [Inheritance](https://github.com/aws/aws-sdk-java/issues/832)
* [Service-side metrics](https://github.com/aws/aws-sdk-java/issues/953)
  ([1](https://github.com/aws/aws-sdk-java/issues/1170),
  [2](https://github.com/aws/aws-sdk-java-v2/issues/703),
  [3](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-417656448))
* [Merging DynamoDB mapper configurations](https://github.com/aws/aws-sdk-java/issues/1201)
* [Cache merged DynamoDB mapper configurations](https://github.com/aws/aws-sdk-java/issues/1235)
* [Create one single type converter interface](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-330616648)
* [Support `@DynamoDBGeneratedUuid` in objects nested within lists](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332958299)
* [Allow annotating fields in addition to methods](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332968651)
* [Non-string keys in maps](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-332974427)
* [Multiple conditions on the same attribute, for save/delete](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-342586344)
* [Persisting public getters from package-private classes](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-343006566)
* [Return modified attributes when doing a save](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-417656448)
* [More direct exposure of scan or filter expressions](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-430993224)
* [Transactions support](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-443308198)
* [Creating an Item from JSON (and vice-versa)](https://github.com/aws/aws-sdk-java-v2/issues/1240)
* 单表多类的直接支持（见
  [此处](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/bp-general-nosql-design.html)）
  （邮件反馈）
* 支持 `Optional`（邮件）
* 异步分页响应支持 `Publisher`（邮件）
* 创建表时支持部分投影（邮件）
* 与 DynamoDB Streams 更好集成（邮件）
* 建表时配置表自动扩缩容（邮件）
* 请求级凭证（邮件）
* 事务隔离包装（邮件）
* 动态属性 — 类型随其他属性值变化，或运行时生成属性名（邮件）
* 结构版本化（邮件）

## 附录 B：替代方案

### 替代方案 1：Level 3 存储库

「Level 2」高层库建立在「Level 1」生成客户端之上、面向具体服务。上文方案即 DynamoDB 的 Level 2 高层库。

「Level 3」高层库聚焦特定客户问题而非特定 AWS 服务。例如客户常用 DynamoDB 存时序数据。替代上文方案的做法是构建多个 Level 3 库，分别面向文档库、时序库等问题域；这些库可把 DynamoDB 作为多种后端之一。

Level 3 库会使用更贴近问题域的词汇（例如文档库用 Document、时序用 Entry），而非传统 DynamoDB 名词（如 Item）；操作也会更贴近该问题域，而非暴露 DynamoDB 全部能力。

该方案更适合熟悉业务问题、对 DynamoDB 较陌生的客户；对熟悉 DynamoDB、希望贴近服务的客户则较差。

**客户反馈**

Java SDK 团队在内、外部收集了客户反馈（[外部讨论](https://github.com/aws/aws-sdk-java-v2/issues/35#issuecomment-468435660)），将该替代方案与提议方案对比。向客户展示如下选项：

> 选项 1：面向 DynamoDB 的客户端，以直接方式整合 1.11.x 的 Documents API 与 DynamoDB Mapper API。

> 选项 2：通用文档数据库客户端，在 DynamoDB、MongoDB 等之上抽象。便于在同一应用中使用多种文档库并迁移，但不再是直接的 DynamoDB 体验。

我们请客户审阅上述选项及
[选项 1 原型](prototype/option-1/sync/Prototype.java)、
[选项 2 原型](prototype/option-2/sync/Prototype.java)，并反馈偏好。

客户反馈摘录：

> 若 \[Amazon] 能做出类似 https://serverless.com/ 或 https://onnx.ai/、让客户摆脱厂商锁定，会是很好的 Think Big & Customer Obsession。若不能，我认为更中立的第三方能把 mapper 做得比 \[Amazon] 更好。

> 是否考虑过向已有项目贡献，例如 Spring Data？https://github.com/derjust/spring-data-dynamodb 

> 两种选项对我们都可行。

> 我认为 \[先做选项 1 再建 Spring Data 插件] 可能比选项 2 更易被更广泛人群采用，可作为迈向 DynamoDB 的垫脚石。

> 我认为选项 2 意义不大。更合理的是选项 1，并悬赏为 spring-data、GORM 等流行数据访问抽象实现模块。

> 或许可实现/支持 JNOSQL 规范 http://www.jnosql.org/

**决策**

根据客户反馈，暂时否决替代方案 1，按提议方案建设。日后 SDK 可能为 DynamoDB 构建 Level 3 抽象，或与 Spring Data、Hibernate OGM、JNoSQL 等现有 Level 3 集成；该 Level 3 抽象可能在底层复用 Level 2 方案。

## 链接

**[Features](features.md)** — enhanced DynamoDB 客户端发布期及之后计划纳入的功能。

**原型**

设计阶段曾创建两个原型接口以收集客户对设计方向的反馈。

* [Prototype 1](prototype/option-1/sync/Prototype.java) — 面向 DynamoDB、聚焦在 Java 中易用 DynamoDB 的 API。
* [Prototype 2](prototype/option-2/sync/Prototype.java) — 与 DynamoDB 解耦的 API，聚焦通用文档数据库抽象，可由 DynamoDB 或其他文档库支撑。
  
**反馈**

* [DynamoDB Mapper Feature Request](https://github.com/aws/aws-sdk-java-v2/issues/35)
  — 跟踪 2.x 中与 DynamoDB mapper 等价功能的客户需求与反馈。
* [DynamoDB Document API Feature Request](https://github.com/aws/aws-sdk-java-v2/issues/36)
  — 跟踪 2.x 中与 DynamoDB document API 等价功能的客户需求与反馈。
