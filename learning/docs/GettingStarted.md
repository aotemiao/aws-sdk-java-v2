## 参与开发 SDK

### 需要了解的事项

- SDK 基于 Java 8 构建
- 使用 [Maven][maven] 作为构建与依赖管理工具
- 大部分服务客户端代码通过[代码生成器][codegen]自动生成

### 开发环境配置提示

若使用 IntelliJ IDEA，以下配置文件会默认作为项目级设置使用：

- [Copyright](https://raw.githubusercontent.com/aws/aws-sdk-java-v2/master/.idea/copyright/AWS_Java_SDK_2_0.xml)

  会在你新建的源文件顶部自动插入许可证头。

- [Code style](https://raw.githubusercontent.com/aws/aws-sdk-java-v2/master/.idea/codeStyles/Project.xml)

  有助于代码符合我们的代码风格规范。

- [Inspections](https://raw.githubusercontent.com/aws/aws-sdk-java-v2/master/.idea/inspectionProfiles/AWS_Java_SDK_2_0.xml)

  有助于保证代码正确并遵循最佳实践。请确保你的修改不会引入新的检查告警。

若在 IDE 中集成了 Checkstyle，也建议使用我们的
[Checkstyle 配置](https://raw.githubusercontent.com/aws/aws-sdk-java-v2/master/build-tools/src/main/resources/software/amazon/awssdk/checkstyle.xml)，
以便在代码旁直接看到违规项。

### 构建

SDK 是标准 Maven 项目，通常使用 `mvn package` 与 `mvn install` 即可构建。

若你在开发[代码生成器][codegen]，务必执行 `mvn install`，而不要只执行 `compile` 或 `test` 等更早的阶段，这样构建才会使用包含你修改的代码生成器 JAR。不确定时，直接使用 `mvn package`。

#### 关闭 Checkstyle / FindBugs

默认情况下，构建会运行 Checkstyle 与 FindBugs 扫描，但会明显拖慢构建。若需在本地快速迭代，可通过属性关闭其中任一项或全部：

```sh
# 同时跳过 Checkstyle 与 FindBugs
$ mvn install -Dfindbugs.skip=true -Dcheckstyle.skip=true
```

### 测试

#### 单元测试

如项目结构所述，测试分为单元测试与集成测试。在常规 `test` 生命周期阶段只会运行单元测试。

```sh
# 运行单元测试
mvn install
```

### 集成测试

**运行集成测试前请注意：需要你提供有效的 AWS IAM 凭证，且测试会真实调用 AWS 服务，会产生账户费用。**

若正在编写集成测试，请尽量评估是否可用带 Mock 响应的单元测试替代。

#### 测试凭证

如上所述，需要可用的 IAM 凭证，且其附加策略须允许测试将要执行的操作。

所有集成测试约定从 `$HOME/.aws/awsTestAccount.properties` 读取凭证：

```
$ cat $HOME/.aws/awsTestAccount.properties

accessKey = ...
secretKey = ...
```

#### 运行集成测试

若要在运行单元测试的同时运行集成测试，必须激活 `integration-tests` profile：

```sh
# 同时运行单元测试与集成测试
mvn install -P integration-tests
```

[maven]: https://maven.apache.org/
[codegen]: https://github.com/aws/aws-sdk-java-v2/blob/master/codegen
