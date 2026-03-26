**设计：** 约定，**状态：** [已采纳](README.md)

# 优先使用静态工厂方法而非构造函数

本页说明用于初始化类的结构与约定。

## 静态工厂方法与构造函数
在以下方面，静态工厂方法优于构造函数：
- 与构造函数相比，静态工厂方法可有更有意义的名称，通过描述返回对象提高可读性。
```java
// 使用静态工厂方法，暗示正在创建使用默认设置的 foobar 提供者。
FoobarProvider defaultProvider = FoobarProvider.defaultFoobarProvider(); 

// 使用构造函数
FoobarProvider defaultProvider = new FoobarProvider(); 
```
- 与不可变类配合时很有用：可复用同一对象，而不必每次调用都新建。
- 静态工厂方法可返回该类的任意子类型，便于在需要时用单独的工厂类返回不同子类。

静态工厂方法也有若干缺点：
- 与构造函数相比，用户可能不那么直观地使用静态工厂方法创建实例，但可通过约定常见方法名（如 `create()`）以及 IntelliJ、Eclipse 等 IDE 的提示与自动补全来改善。
- 没有 public 或 protected 构造函数的类无法被继承，但这也有助于我们更多采用组合而非继承。

总体而言，应优先使用静态工厂方法而非构造函数。

## 示例
```java
public class DefaultCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider(builder());

    private DefaultCredentialsProvider(Builder builder) {
        this.providerChain = createChain(builder);
    }

    public static DefaultCredentialsProvider create() {
        return DEFAULT_CREDENTIALS_PROVIDER;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder { 
      // ...
    }
    // ...
}


// 有两种方式创建新实例
DefaultCredentialsProvider defaultCredentialsProvider1 = DefaultCredentialsProvider.create();
DefaultCredentialsProvider defaultCredentialsProvider2 = DefaultCredentialsProvider.builder().build;
```
## 命名约定
静态工厂方法的命名约定：
- 创建新实例时使用 `create()`、`create(params)`
例如：`DynamoDBClient.create()`
- 返回带默认设置的实例时使用 `defaultXXX()`。
例如：`BackoffStrategy.defaultStrategy()`
