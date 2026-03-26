# 发布功能

早期客户-facing API 原型见 [Prototype 1](prototype/option-1/sync/Prototype.java)。

* `software.amazon.aws:dynamodb-all` 模块，含可创建生成式 DynamoDB 与 enhanced DynamoDB 客户端的网关类。
* `software.amazon.awssdk:dynamodb-enhanced` 模块，含全部 DynamoDB enhanced 客户端类。
* 支持以下 DynamoDB 控制面资源：表、全局表、全局二级索引。
* 支持以下 DynamoDB 数据面操作：get、put、query、scan、delete、update、batchGet、batchWrite、transactGet、transactWrite。
* 支持对象投影。
* 以 `Item` 抽象替代生成客户端中的 `Map<String, AttributeValue>`。
* 以 `ItemAttributeValue` 抽象替代生成客户端中的 `AttributeValue`。
* 内置 Java 类型与 `ItemAttributeValue` 的转换器。
* 自定义 Java bean 与 `ItemAttributeValue` 的转换器。
* 可注册自定义类型转换器。
* 暴露底层客户端元数据，如消耗容量与指标。

# 发布后功能

* 支持自定义 Java bean 的继承。
* 支持不符合 Java bean 规范的自定义 POJO 与 `ItemAttributeValue` 的转换器。
* 支持以下 DynamoDB 控制面资源：备份、连续备份、限额、TTL、标签。
  
# 相对 1.11.x 的客户诉求变更

* 所有内置 Java 类型应无需类型转换器即可支持。
* 应只有一种可扩展的类型转换机制，以覆盖任何合理、可预见的客户场景。
* 高层库应支持客户的不可变对象表示。
* 高层库应支持对象继承。
* 高层库应支持事务写。
* 高层库应支持非阻塞访问模式。
* 高层库应暴露底层客户端元数据，如消耗容量与指标。

# 常见问题

**为何「加载-修改-保存」场景下的脏数据跟踪与持久化不在功能列表中？**

从实现角度，这要求 SDK 跟踪哪些字段被修改。该复杂度更适合放在（未来的）更高一层抽象；本库仅聚焦类型转换问题。
