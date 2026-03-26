# 代码生成指南

## 目录
- [概述](#概述)
- [架构概览](#架构概览)
- [Poet 规范开发](#poet-规范开发)
- [生成器任务组织](#生成器任务组织)
- [生成代码标准](#生成代码标准)
- [模型处理](#模型处理)
- [定制与扩展](#定制与扩展)
- [代码示例与参考](#代码示例与参考)

## 概述

本文说明如何在 AWS SDK for Java v2 中开发与维护代码生成组件。代码生成是 SDK 的关键部分，负责将服务模型通过 JavaPoet 转换为 Java 客户端代码。

## 架构概览

AWS SDK v2 代码生成系统围绕若干核心组件构建：

### 核心组件
- **CodeGenerator**：协调生成过程的主编排器
- **IntermediateModel**：用于代码生成的已处理服务模型
- **Poet Specs**：基于 JavaPoet、定义生成类的规范
- **Generator Tasks**：并行执行、生成特定代码产物的任务单元

### 与 JavaPoet 的集成
SDK 使用 Square 的 JavaPoet 库进行类型安全的 Java 代码生成：
- **ClassSpec**：定义生成类规范的接口
- **TypeSpec**：JavaPoet 的类/接口/枚举构建器
- **MethodSpec**：方法生成规范
- **FieldSpec**：字段生成规范

## Poet 规范开发

### 创建 ClassSpec 实现

新增代码生成器时，实现 `ClassSpec` 接口：

```java
public class MyGeneratorSpec implements ClassSpec {
    @Override
    public TypeSpec poetSpec() {
        return TypeSpec.classBuilder(className())
                      .addAnnotation(PoetUtils.generatedAnnotation())
                      .addModifiers(Modifier.PUBLIC)
                      .build();
    }

    @Override
    public ClassName className() {
        return ClassName.get("com.example", "GeneratedClass");
    }
}
```

#### ClassSpec 最佳实践
- 始终使用 `PoetUtils.generatedAnnotation()` 包含 `@Generated` 注解
- 使用能反映生成组件用途的有意义的类名
- 当静态导入能提高可读性时，实现 `staticImports()`
- 每个 poet 规范聚焦单一职责

### JavaPoet 代码生成模式

#### 类型安全
- 引用已有类时使用 `ClassName.get()`
- 泛型使用 `ParameterizedTypeName.get()`
- 泛型形参使用 `TypeVariableName`
- 通配符使用 `WildcardTypeName`

#### 代码块构造
```java
CodeBlock.builder()
    .add("$T.<$T>builder($T.$L)\n", 
         SdkField.class, fieldType, 
         MarshallingType.class, marshallingType)
    .add(".memberName($S)\n", memberName)
    .add(".build()")
    .build();
```

#### 方法生成
```java
MethodSpec.methodBuilder("methodName")
    .addModifiers(Modifier.PUBLIC)
    .returns(returnType)
    .addParameter(paramType, "paramName")
    .addStatement("return $L", expression)
    .build();
```

### Poet 工具

#### PoetUtils 辅助方法
- `generatedAnnotation()`：标准 @Generated 注解
- `createClassBuilder()`：带 @Generated 的类构建器
- `createInterfaceBuilder()`：带 @Generated 的接口构建器
- `addJavadoc()`：安全添加 JavaDoc 并正确转义
- `buildJavaFile()`：将 ClassSpec 转为 JavaFile 并处理静态导入

## 生成器任务组织

### 任务层次
代码生成按层次组织任务：
- **AwsGeneratorTasks**：顶层协调
- **CommonGeneratorTasks**：共享模型与客户端生成
- **AsyncClientGeneratorTasks**：异步客户端相关生成
- **PaginatorsGeneratorTasks**：分页器生成
- **EventStreamGeneratorTasks**：事件流支持
- **WaitersGeneratorTasks**：Waiter 生成

### 任务实现模式
```java
public class MyGeneratorTask extends GeneratorTask {
    @Override
    public void compute() {
        // Generate code using JavaPoet
        ClassSpec spec = new MyClassSpec(model);
        new PoetGeneratorTask(outputDir, fileHeader, spec).compute();
    }
}
```

## 生成代码标准

生成代码必须遵守现有 AWS SDK for Java v2 的全部指南与标准。完整列表见 [指南索引](README.md)。

## 模型处理

### Intermediate Model 用法
`IntermediateModel` 提供已处理的服务定义：
- **ShapeModel**：服务数据结构
- **MemberModel**：结构成员/字段
- **OperationModel**：服务操作
- **MetadataModel**：服务元数据与配置

## 定制与扩展

### 服务特定定制
- 使用 `CustomizationConfig` 表达服务特定行为
- 在服务专属文件中记录所有定制

### 扩展点
- 为新生成器实现自定义 `ClassSpec`
- 扩展现有生成器任务以增加功能
- 使用 `PoetExtension` 做模型到类名的映射
- 使用 `NamingStrategy` 保持命名一致

## 代码示例与参考

### 现有 ClassSpec 实现
JavaPoet 代码生成模式的实践可参考：

#### 模型生成
- **ShapeModelSpec**：`codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ShapeModelSpec.java`
  - 演示带 SdkField 元数据的字段生成
  - 展示编组/解组用的 trait 应用
  - 实现静态字段初始化模式

- **ModelBuilderSpecs**：`codegen/src/main/java/software/amazon/awssdk/codegen/poet/model/ModelBuilderSpecs.java`
  - Builder 接口与实现生成
  - 联合类型与枚举生成处理
  - 流式 setter 方法模式

#### 任务组织
- **AwsGeneratorTasks**：`codegen/src/main/java/software/amazon/awssdk/codegen/emitters/tasks/AwsGeneratorTasks.java`
  - 组织生成的组合任务模式
  - 并行执行协调

- **PoetGeneratorTask**：`codegen/src/main/java/software/amazon/awssdk/codegen/emitters/PoetGeneratorTask.java`
  - ClassSpec 与文件输出的集成
  - 生成过程中的错误处理

#### 工具类
- **PoetUtils**：`codegen/src/main/java/software/amazon/awssdk/codegen/poet/PoetUtils.java`
  - 常见 JavaPoet 模式与工具
  - 类型创建辅助与注解管理
  - 带静态导入的 JavaFile 构建

- **CodeGenerator**：`codegen/src/main/java/software/amazon/awssdk/codegen/CodeGenerator.java`
  - 主编排与校验流程
  - 模型处理与任务执行模式

### 校验与测试模式

#### 基于 Fixture 的测试
使用静态资源 fixture 校验生成代码输出，模式见 `AsyncClientClassTest`：

```java
// From AsyncClientClassTest.java
@Test
public void asyncClientClassRestJson() {
    AsyncClientClass asyncClientClass = createAsyncClientClass(restJsonServiceModels(), false);
    assertThat(asyncClientClass, generatesTo("test-json-async-client-class.java"));
}
```

**测试模式要点：**
- **静态 Fixture**：将期望生成代码存为 `src/test/resources/` 下的 `.java` 文件
- **PoetMatchers**：使用 `generatesTo()` 匹配器对比生成结果与 fixture
- **Fixture 组织**：按组件类型（客户端、模型等）分组
- **多场景**：测试不同服务模型与配置
- **空白规范化**：匹配器忽略空白差异以提高鲁棒性

**Fixture 目录结构：**
```
codegen/src/test/resources/software/amazon/awssdk/codegen/poet/client/
├── test-json-async-client-class.java          # Expected async client output
├── test-query-client-class.java               # Expected sync client output  
├── test-custompackage-async.java              # Custom package scenarios
```
