**设计：** 新功能，**状态：**
[开发中](../../../../../README.md)

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

## 提议方案

AWS SDK for Java 将新增「enhanced DynamoDB client」，作为生成式 DynamoDB API 数据访问部分的替代。发布初期不支持「建表」等控制面操作，日后可能补充。

该 enhanced 客户端通过以下方式让 Java 客户更易使用 DynamoDB：
1. 支持 Java 对象与 DynamoDB 项之间的转换
2. 支持 Java 内置类型（如 `Instant`）与 DynamoDB 属性值类型之间的转换
3. 直接支持 DynamoDB 全部数据面操作
4. 使用与 DynamoDB 相同的动词与名词

## 实现概览

**新客户端**

将新增两个客户端类：`DynamoDbEnhancedClient` 与 `DynamoDbEnhancedAsyncClient`。它们包装生成的 `DynamoDbClient` 与 `DynamoDbAsyncClient`，在生成客户端能力之上提供附加功能。

```java
DynamoDbEnhancedClient enhancedClient = 
    DynamoDbEnhancedClient.builder()
                          .dynamoDbClient(DynamoDbClient.create())
                          .build();
```

**表抽象**

`DynamoDbEnhancedClient` 提供 `Table` 与 `MappedTable`；`DynamoDbEnhancedAsyncClient` 提供 `AsyncTable` 与 `AsyncMappedTable` 抽象。

这些「表」上的操作与底层 DynamoDB 客户端的数据面操作一一对应。例如存在 `DynamoDbClient.putItem`，则也有 `Table.putItem`。

`Table` 与 `AsyncTable` 操作「项」（见下文）。`MappedTable` 与 `AsyncMappedTable` 操作「对象」（见下文）。`Table` 与 `MappedTable` 同步返回结果；`AsyncTable` 与 `AsyncMappedTable` 异步返回。

```java
Table booksTable = enhancedClient.table("books");
booksTable.putItem(...);

MappedTable mappedBooksTable = enhancedClient.mappedTable("books");
mappedBooksTable.putItem(...);
```
 
**项抽象**

`Table` 与 `AsyncTable` 上的操作针对 `Item`。`Item` 是对生成的 `Map<String, AttributeValue>` 的用户友好表示，支持 Java 内置类型与 DynamoDB `AttributeValue` 类型之间的自动转换。

```java
booksTable.putItem(Item.builder()
                       .putAttribute("isbn", "0-330-25864-8")
                       .putAttribute("title", "The Hitchhiker's Guide to the Galaxy")
                       .putAttribute("creationDate", Instant.now())
                       .build());
```

`Table` 与 `AsyncTable` 可视为 1.11.x DynamoDB Document 客户端的替代。

**对象抽象**

`MappedTable` 与 `AsyncMappedTable` 上的操作针对 Java 对象（首版为 Java bean）。enhanced 客户端自动将其转为生成的 `Map<String, AttributeValue>`。`MappedTable` 与 `AsyncMappedTable` 很可能在实现细节上使用 `Table` 与 `AsyncTable`。

```java
Book book = new Book();
book.setIsbn("0-330-25864-8");
book.setTitle("The Hitchhiker's Guide to the Galaxy");
book.setCreationDate(Instant.now());
mappedBooksTable.putItem(book);
```

`MappedTable` 与 `AsyncMappedTable` 可视为 1.11.x DynamoDB Mapper 客户端的替代。

**类型转换**

映射器的核心能力是将常见 Java 结构（如 Java bean）与类型（如 `Instant`、`Number`）转为 DynamoDB 属性值。

转换依据客户指定的类型进行。例如，客户将某 `Item` 属性指定为任意 `Number` 时，SDK 会自动转为 DynamoDB number。

客户可在 `Item` 或 `DynamoDbEnhanced[Async]Client` 级别配置所用类型转换器，从而：支持尚未内置的类型；改变某 Java 类型对应的 DynamoDB 类型（例如将 `Instant` 存为字符串而非数字）；或为自定义 POJO 增加非 bean 的转换逻辑；或为特定对象类型提供比内置反射转换更高效的硬编码转换器。

## 功能

**发布时功能**

下列功能计划在库首发时纳入。

1. 支持全部现有数据面操作：get、put、query、update、scan、delete、batch get、batch put、transaction get、transaction put。
2. 支持上文所述的 `[Async]Table` 与 `[Async]MappedTable`。
3. 在 `[Async]MappedTable` 中支持基于 bean 的表示。
4. 为当前 [Joda Convert](https://www.joda.org/joda-convert/) 所支持的全部 Java 内置类型提供类型转换器。

| API | 功能 | 开发 | 可用性研究 |
| --- | --- | --- | --- |
| Item | Get | Done | |
| | Put | Done | |
| | Query | | |
| | Update | | |
| | Scan | | |
| | Delete | | |
| | Batch Get | | |
| | Batch Put | | |
| | Transaction Get | | |
| | Transaction Put | | |
| Object | Get | | |
| | Put | | |
| | Query | | |
| | Update | | |
| | Scan | | |
| | Delete | | |
| | Batch Get | | |
| | Batch Put | | |
| | Transaction Get | | |
| | Transaction Put | | |
| All | Type Support | In Progress | |

**发布后功能**

1. 在 `[Async]MappedTable` 中支持继承。
2. 在 `[Async]MappedTable` 中支持不可变对象。
3. 在 `[Async]Table` 与 `[Async]MappedTable` 中支持投影表达式。
4. 支持 DynamoDB 提供的 API 指标（如消耗容量）。
5. `software.amazon.aws:dynamodb-all` 模块，自动包含全部 AWS DynamoDB 构件，提升客户端可发现性。

**不包含的功能**

下列功能不计划在首发纳入（未来可能增加）。

1. 控制面操作（如创建/删除表）。*排除理由：* 测试可通过控制台或底层 SDK；生产环境宜通过 AWS CDK 或 CloudFormation。
2. 版本化与 UUID 注解。*排除理由：* 高于 enhanced 客户端当前聚焦的「类型转换」目标；宜在 enhanced 客户端之上构建。
   
**需求功能**

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
  （邮件）
* 支持 `Optional`（邮件）
* 异步分页响应支持 `Publisher`（邮件）
* 创建表时支持部分投影（邮件）
* 与 DynamoDB Streams 更好集成（邮件）
* 建表时配置表自动扩缩容（邮件）
* 请求级凭证（邮件）
* 事务隔离包装（邮件）
* 动态属性 — 类型随其他属性值变化，或运行时生成属性名（邮件）
* 结构版本化（邮件）

## 附录 A：替代方案

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
