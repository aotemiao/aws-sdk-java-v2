**设计：** 新功能，**状态：** [已发布](../../../../../services-custom/dynamodb-enhanced/README.md)

## 问题
DynamoDB Enhanced 的表操作 `updateItem()` 在提供带键属性的 POJO 时，支持通过覆盖部分或全部属性来创建或更新项。相比之下，Enhanced 操作底层的 DynamoDB [updateItem](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_UpdateItem.html) 通过 [UpdateExpression 语法](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.UpdateExpressions.html) 支持更广的能力。

本文提议让用户提供 update expression，以利用底层表达式能力并实现原子计数器等场景。

### 需求功能
与 DynamoDB Enhanced UpdateItem 相关的客户诉求：

1. 递增/递减数值属性以支持原子计数等场景
2. 向列表添加元素
3. 取消设置/置空特定属性而不替换整项
4. 调整返回值行为

示例 GitHub issue：https://github.com/aws/aws-sdk-java-v2/issues/2292

## 当前能力
调用 `updateItem` 时，enhanced 客户端将所供 POJO 转为底层 UpdateExpression 语法。目前仅支持少数动作：
- REMOVE — 删除属性
- SET — 设置整个属性

## 提议方案
enhanced 客户端除向 `updateItem` 提供常规 POJO 外，还允许用户提供自定义 update expression。
### Enhanced Client UpdateExpression API
在 enhanced 客户端中编写的 `UpdateExpression` 在更高抽象层建模 DynamoDB 语法，以便合并与分析表达式。创建 `UpdateExpression` 时，创建一个或多个 `UpdateAction`（`AddAction`、`SetAction`、`RemoveAction`、`DeleteAction`）并加入 `UpdateExpression` builder。

~~~
SetAction setAction = SetAction.builder()
                               .path("#attr1_ref")
                               .value(":new_value")
                               .putExpressionName("#attr1_ref", "attr1")
                               .putExpressionValue(":new_value", newValue)
                               .build();
                               
UpdateExpression updateExpression = UpdateExpression.builder()
                                                    .addAction(setAction)
                                                    .build();
~~~
*path = 属性名或底层 API 支持的另一表达式。*<br>
*value = 路径取值，也可包含底层表达式。*<br>
*expressionNames =（可选）将名称占位符映射到属性名。*<br>
*expressionValues = 将值占位符映射到属性值。*<br>

### 应用 UpdateExpression
#### Schema 级
Schema 级 `UpdateExpression` 适用于每次访问数据库都要执行的相同动作，例如原子计数。扩展框架支持在 `WriteModification` 中输出 `UpdateExpression`。

在实现 [DynamoDbEnhancedClientExtension](https://github.com/aws/aws-sdk-java-v2/blob/feature/master/DynamoDBenhanced-updateexpression/services-custom/dynamodb-enhanced/src/main/java/software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedClientExtension.java) 的扩展类中：
~~~
WriteModification.builder()
                 .updateExpression(updateExpression)
                 .build();
~~~
将扩展加入扩展链：
~~~
DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                              .dynamoDbClient(getDynamoDbClient())
                                                              .extensions(MyExtension.create())
                                                              .build();

DynamoDbTable<Record> table = enhancedClient.table("some-table-name"), TableSchema.fromClass(SomeRecord.class));
~~~


#### 请求级
请求级 `UpdateExpression` 允许在单次调用时修改记录，例如向列表追加或删除属性，通过在请求本身上附加 update expression 实现。

**注意：** 初始版本不支持该能力。

### 将 enhanced UpdateExpression 转为底层
在创建底层请求时由 enhanced 客户端在 `UpdateItemOperation` 中完成。使用 `UpdateExpressionConverter` 将 `UpdateExpression` 转为 `Expression`，再转为 DynamoDB 期望的字符串格式。

### 优先级与合并规则
多个扩展均可返回 `UpdateExpression`，需在扩展链中合并，使最终的扩展 `UpdateExpression` 到达 `UpdateItemOperation`。在操作内部，扩展表达式必须与为请求 POJO 生成的表达式合并。

[PR #2926](https://github.com/aws/aws-sdk-java-v2/pull/2926) 更详细说明解析最终 `UpdateExpression` 的难点。主要问题之一是在同时使用扩展与请求配置时， reconcile 用户体验并使默认配置下结果合理。

#### 在扩展链中合并 UpdateExpression

扩展合并将链中较后的表达式追加到已有 `UpdateExpression`（解析前 `UpdateExpression` 仅为一组集合）。

问：若链中较后的扩展修改同一属性会怎样？<br>
答：两者的更新动作都会进入 `UpdateExpression`，解析阶段失败。是否需要更多支持？

问：扩展能否看到扩展链中先前的 `UpdateExpression`？<br>
答：扩展的输入上下文目前不包含 `UpdateExpression`，故扩展无法查看先前表达式（扩展仍可查看对 transact item 的更新）。

#### 合并扩展与请求 POJO 的 UpdateExpression

问：扩展表达式与请求级 POJO 表达式谁优先？（原文 reqest 为 request 笔误）<br>
答：目前直接相加合并，例外是：对 POJO 表达式自动生成的属性删除，若已被扩展 `UpdateExpression` 显式修改，则过滤掉。因默认 `ignoreNulls` 为 false，否则 REMOVE 会与扩展属性冲突。

问：是否应对 POJO 的 SET 做类似处理？<br>
答：取决于如何看待请求级 POJO 更新与扩展更新。一方面请求级常优先；另一方面需防止扩展被覆盖。当前未实现。

问：若 POJO 设置的属性也被扩展修改会怎样？<br>
答：解析时抛异常，防止用户覆盖扩展维护的属性。

## 附录 B：替代方案

### 设计替代：更流式的 UpdateExpression API 
该替代方案中 API 与更新动作更流式，用户代码更少：
~~~
UpdateExpression updateExpression1 = UpdateExpression.builder()
                                                     .remove("attr1")                              
                                                     .set("attr1", value1, updateBehavior)
                                                     .build();
~~~
若直接添加动作，则暴露如下方法：
~~~
UpdateExpression updateExpression2 = UpdateExpression.builder()
                                                     .removeAction(UpdateAction.remove("attr1"))
                                                     .setAction(SetUpdateAction.addWithStartValue(attr2, delta, start))
                                                     .addAction(UpdateAction.appendToList("attr1", 3, myListAttributeValue))
                                                     .addAction(UpdateAction.appendToList("attr1", 3, myListAttributeValue))
                                                     .build();
~~~

**决策**

因高度建模的 API 灵活性不足，难以跟进 DynamoDB 底层语法变更，故否决该替代。

### 设计替代：单一 UpdateAction 类
用类型字段区分不同动作：
~~~
UpdateAction updateAction = 
          UpdateAction.builder()
                      .type(UpdateActionType.REMOVE)
                      .attributeName(attributeName)
                      .expression(keyRef(attributeName))
                      .build();
~~~

**决策**

否决，维持当前设计。
