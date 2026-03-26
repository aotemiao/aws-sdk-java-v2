# 客户端配置

本页说明客户端配置对象的结构与约定。客户端配置对象指用于配置 AWS 客户端构建器（builder）的任意对象。

## 示例

本节通过一个示例配置类结构，概要说明各组成部分。

```Java
/**
 * 配置说明 // (1)
 */
@Immutable
@ThreadSafe // (2)
public final class SdkConfiguration // (3)
        implements ToCopyableBuilder<SdkConfiguration.Builder, SdkConfiguration> { // (4)
    private final String option; // (5)

    /**
     * @see #builder() // (6)
     */
    private SdkClientConfiguration(DefaultSdkConfigurationBuilder builder) {
        this.option = builder.option;
    }

    public static Builder builder() {
        return new DefaultSdkConfigurationBuilder();
    }

    /**
     * @see #Builder#option(String) // (7)
     */
    public String option() {
        return this.option;
    }

    @Override
    public ClientHttpConfiguration.Builder toBuilder() {
        return builder().option(option);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, SdkConfiguration> { // (8)
        /**
         * 配置项说明 // (9)
         */
        Builder option(String option);
    }

    private static final class DefaultSdkConfigurationBuilder implements Builder { // (10)
        private String option;

        @Override
        public Builder option(String option) { // (11)
            this.option = option;
            return this;
        }

        public void setOption(String option) { // (12)
            this.option = option;
        }

        @Override
        public SdkConfiguration build() {
            return new SdkConfiguration(this);
        }
    }
}
```

1. 应详细说明用户在此对象中可能遇到的配置项类型。
2. 配置对象应为 `@Immutable`，因而也是 `@ThreadSafe`。
3. 配置类应声明为 `final`，禁止被继承扩展。
4. 配置类应实现 `ToCopyableBuilder`，以便能转换回构建器。
5. 所有配置字段应为 `private final`，禁止重新赋值。
6. 配置构造函数应为 `private`，强制通过 `Builder` 创建；并引导读者通过 `builder()` 创建实例。
7. 每个配置字段对应一个「获取」方法，方法名必须与字段名完全一致。
8. 每个构建器应有独立接口，便于在自动补全中隐藏部分公开方法（见下文）。
9. 每个选项应有详细说明，包括作用及**为何**用户可能希望修改默认值。
10. 应提供 `Builder` 接口的 `private static final` 实现，且不在配置类作用域外暴露。
11. 每个选项应有一个在构建器中修改该字段的「设置」方法，方法名必须与字段名完全一致。
12. 每个选项应有 JavaBean 风格的 setter，以便通过基于 `Inspector` 的框架（如 Spring XML）反射配置。

### 配置字段

本节说明配置字段的语义。

1. 配置字段必须**不可变**。
    1. 字段必须标记为 `final`。
    2. 可变类型（如 `List`、`Set`）在通过「获取」方法返回时，应包装为不可修改类型（例如 `Collections.unmodifiableList`）。
    3. 可变类型在「设置」方法中应进行拷贝，避免通过修改原对象间接修改配置。
2. 配置字段必须是**引用类型**。不应使用 `boolean`、`int` 等基本类型，因其无法表达「未配置」。
3. 配置字段名**不应以动词开头**（例如应使用 `config.redirectEnabled()` 而非 `config.enableRedirect()`），以免「获取」方法被误认为会修改状态。

集合类型（`List`、`Set`、`Map`）的特别说明：

1. 集合类型字段名必须为复数。
2. 应提供两种修改集合的方式。
  - 与复数字段同名的方法：允许传入整个集合，应覆盖构建器中当前已配置的值。
  - 允许向集合添加单个条目的方法。
    - 若为列表，方法名应使用 `add` 前缀，例如：`addApiName`
    - 若为映射，方法名应使用 `put` 前缀，例如：`putHeader`

```Java
public interface Builder {
    
    /**
     *  Sets options.
     *  
     *  <p>
     *  This overrides any option values already configured in the builder.
     */
    Builder options(List<String> options);
    
    /**
     * Add a single option to the collection.
     */
    Builder addOption(String option);

    /**
     *  Sets headers to be set on the HTTP request. 
     *  
     *  <p>
     *  This overrides any header values already configured in the builder.
     */
    Builder headers(Map<String, String> headers);
    
    /**
     *  Add a single header to be set on the HTTP request.
     *  
     *  <p>
     *  This overrides any values already configured with this header name in the builder.
     */
    Builder putHeader(String key, String value);
}
```
