**设计：** 新功能，**状态：** [提议中](../../README.md)

# 标记联合（Tagged Unions）

## 动机
服务团队多年来通过要求「在任意时刻仅有一个成员被设置」的结构来定义 AWS 服务中的标记联合，但 AWS SDK for Java 尚未对其提供一等公民支持。

标记联合的典型例子是 DynamoDB 的 `AttributeValue`：

```java
class AttributeValue {
    private String s;
    private String n;
    private SdkBytes b;
    private List<String> ss;
    private List<String> ns;
    private List<SdkBytes> bs;
    
    // ...
}
```

目前客户需要自行对取值做临时匹配，先判断联合中是哪一类值，再在分支代码中使用。我们不应让客户重复这套工作，而应提供抽象，使其易于使用这些值。

## 目标

本项目的目标是为 2.x SDK 中现有及未来的联合类型提供向后兼容、对客户友好的抽象。

## 既有实践

SDK 中至少已在两处对外实现友好的标记联合：
1. `software.amazon.awssdk.core.document.Document` 类型。
2. 事件流类型，例如 Lex 的 `software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequestEventStream` 类型。

以及至少一处内部用法：
1. `software.amazon.awssdk.protocols.jsoncore.JsonNode` 类型。

## 联合类型定义

我们将遵循 `Document` 与 `JsonNode` 已确立的模式，因为 `StartConversationRequestEventStream` 所用模式与现有类型不兼容。

现有结构主要包含以下方法：
1. 用于创建结构并设置互斥成员的静态 `builder()` 方法。
2. 联合中每个成员的「获取」方法。
3. 每个集合或映射成员的「是否存在」方法，用于区分缺失与空值。

除上述方法外，新建与现有联合类型还将增加：
1. 静态 `T fromN(...)` 方法，用单个独占成员初始化结构。例如 `AttributeValue s = AttributeValue.fromS("string")`
2. `X.Type type()` 方法，判断联合中包含的类型。例如 `AttributeValue.Type type = getItemResponse.item().type()`

例如，包含 `String s`、`String n` 与 `SdkBytes b` 的 `AttributeValue` 联合类型将具有如下公开方法：
```java
class AttributeValue {
    static AttributeValue.Builder builder();
    static fromS(String s);
    static fromN(String n);
    static fromB(SdkBytes b);
    
    String s();
    String n();
    SdkBytes b();

    AttributeValue.Type type();
    
    enum Type {
        S,
        N,
        B,
        UNKNOWN_TO_SDK_VERSION
    }
    
    class Builder {
        // ...
    }
    
    // ...
}
```

## 客户体验

以下示例展示联合类型变更前后的客户代码：

### 创建联合类型

**变更前**
```java
AttributeValue.builder().s("foo").build()
```

**变更后**
```java
AttributeValue.fromS("foo")
```

### 读取未知类型的字段

**变更前**
```java
String result;
if (attributeValue.s() != null) { result = attributeValue.s(); }
else if (attributeValue.n() != null) { result = attributeValue.n(); }
else if (attributeValue.b() != null) { result = attributeValue.b().asUtf8String(); }
else { result = null; }
```

**变更后**
```java
// (Java 17+)
String result = switch (attributeValue.type()) {
    case S -> attributeValue.s();
    case N -> attributeValue.n();
    case B -> attributeValue.b().asUtf8String();
    default -> null;
}

// Java (8-16)
String result = null;
switch (attributeValue.type()) {
    case S: result = attributeValue.s(); break;
    case N: result = attributeValue.n(); break;
    case B: result = attributeValue.b().asUtf8String(); break;
}
```

### 使用未知类型的字段

**变更前**
```java
if (attributeValue.s() != null) { System.out.println(attributeValue.s()); }
else if (attributeValue.n() != null) { System.out.println(attributeValue.n()); }
else if (attributeValue.b() != null) { System.out.println(attributeValue.b().asUtf8String()); }
```

**变更后**
```java
attributeValue.visit(new AttributeValue.VoidVisitor() {
    void visitS(String s) { System.out.println(s); }
    void visitN(String n) { System.out.println(n); }
    void visitB(SdkBytes b) { System.out.println(b.asUtf8String()); }
)

// (Java 17+)
System.out.println(switch (attributeValue.type()) {
    case S -> attributeValue.s();
    case N -> attributeValue.n();
    case B -> attributeValue.b().asUtf8String();
    default -> null;
});

// Java (8-16)
switch (attributeValue.type()) {
    case S: System.out.println(attributeValue.s()); break;
    case N: System.out.println(attributeValue.n()); break;
    case B: System.out.println(attributeValue.b().asUtf8String()); break;
}
```
