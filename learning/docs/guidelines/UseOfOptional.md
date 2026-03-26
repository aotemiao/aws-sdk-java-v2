**设计：** 约定，**状态：** [已采纳](README.md)

## Optional 的使用

本页说明在 AWS SDK for Java 2.x 中使用 `java.util.Optional` 的一般准则。

- 若结果**绝不会**为 `null`，**在任何情况下都不得**使用 `Optional`。
- 对于返回类型：
  - 当调用方**不明显**能否得到非空结果时，**应**使用 `Optional`，例如 [SdkResponse.java](https://github.com/aws/aws-sdk-java-v2/blob/aa161c564c580ced4a0381d3ed7d4d13120916fc/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkResponse.java#L59-L61) 中的 `public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz)`
  - 在生成的服务模型类（如服务 Builder 或 POJO）的「getter」中**不得**使用 `Optional`。
- 成员变量：**不应**使用 `Optional`，例如：`private final Optional<String> field;`
- 方法参数：**不得**使用 `Optional`，例如：`private void test(Optional<String> value)`


参考资料：

- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
