**设计：** 新功能，**状态：** 设计中

## 问题

在 DynamoDB 中，[item（项）](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html) 是属性的集合，每个属性有名称与取值。Aws-sdk-java 1.x 提供 Document API 访问这些项，用户无需掌握整项的完整 schema 即可访问。Aws-sdk-java 2.x 尚无同等能力。本文提议通过 enhanced dynamodb 客户端以文档形式访问 DDB 项的机制。

### 需求功能
Aws-sdk-java 2.x 应提供与 1.x 类似的 Document API，包括：

1. 在无需使用 DynamoDB Mapper 的情况下访问 DynamoDB 复杂数据模型的 API。例如 JSON 与 DynamoDB 项互转的 API。
2. 操纵各属性值半结构化数据的 API。例如将 `AttributeValue` 作为字符串集、数字集、字符串列表、数字列表等访问的 API。
3. 支持将 DynamoDB 元素直接以 Document 读写。

示例 GitHub issue：https://github.com/aws/aws-sdk-java-v2/issues/36

## 当前能力
Aws-sdk-java 2.x 已通过 Mapper 客户端提供中阶 DynamoDB 映射/抽象。使用这些 mapper 时，用户需在创建映射表时定义完整表 schema。

## 命名约定
本文中的类名与 API 名称并非最终版，后续评审可能调整。

## 提议方案

在现有 enhanced 客户端中新增 `DocumentSchema`。创建映射表时只需定义分区键与排序键，即可将 DynamoDB 表项取回为 Document。用户随后可通过 getter 访问文档中的属性值；也可新建 Document 并通过 Document 的 builder API 写入映射表。

### Enhanced Client 表 Schema 创建 API
在构建器中提供 `partitionKey`、`sortKey` 与可选的 `attributeConverterProviders` 即可创建 `DocumentSchema`。若 `TableSchema` 未提供 `AttributeConverterProvider`，Document 表 schema 将使用默认转换器提供者。
~~~java
 // Existing way of creating enhanced client
 DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().build();

// New API in TableSchema to create a DocumentTableSchema 
DocumentTableSchema documentTableSchema =
    TableSchema.documentSchemaBuilder()
               .addIndexPartitionKey(primaryIndexName(), "sample_hash_name", AttributeValueType.S)
               .addIndexSortKey("gsi_index", "sample_sort_name", AttributeValueType.N)
               .addAttributeConverterProviders(cutomAttributeConverters)
               .build();
                       
 // Existing API to access DynamoDB table.    
 DynamoDbTable<EnhancedDocument> documentTable = enhancedClient.table("table-name", documentTableSchema);
~~~
*`addAttributeConverterProviders`：在 SDK 默认提供的转换器之外追加自定义 attribute converter providers*<br>

### 从 DDB 表访问 Document
#### 从表读取文档
经 `DocumentSchema` 映射的表将项返回为 `EnhancedDocument`，再用其获取属性值。

~~~java
// Creating a document which defined primary key of the item needs to be retrieved
EnhancedDocument hashKeyDocument = EnhancedDocument.builder()
                                                    .addString("sample_hash_name", "sample_value")
                                                    .build();
// Retrieving from existing Get operation.
EnhancedDocument retrievedDocument = documentTable.getItem(hashKeyDocument);
    
// Retrieving from existing Get operation
EnhancedDocument documentTableItem = documentTable.getItem(
                                        EnhancedDocument.builder()
                                                        .addString("sample_hash_name", "sample_value")
                                                        .build());

// Accessing an attribute from document using generic getter.
Number sampleSortvalue = documentTableItem.get("sample_sort_name", EnhancedType.of(Number.class));

// Accessing an attribute from document using specific getters.
sampleSortvalue = documentTableItem.getNumber("sample_sort_name"); 

// Accessing an attribute of custom class using custom converters.
CustomClass customClass = documentTableItem.get("custom_nested_map", new CustomAttributeConverter()));

// Accessing Nested set 
Set<List<String>> stringSet = documentTableItem.get("string_set", new EnhancedType<Set<List<<String>>>(){}));
~~~


#### 向 DDB 表写入文档
`EnhancedDocument` 提供 builder 方法以创建可写入 `DocumentSchema` 映射表的文档。

~~~java

// Creating a document from Json input.   
EnhancedDocument  documentFromJson = EnhancedDocument.fromJson(("{\"sample_hash_name\": \"sample_value_2\"}"));
// put to dynamo db table
documentTable.putItem(documentFromJson);

// Creating a document from EnhanceDocumentBuilders    
EnhancedDocument documentFromBuilder = EnhancedDocument.builder()
                                                       .addString("sample_hash_name", "sample_value_2")
                                                       .addNumber("sample_sort_name", 111)
                                                       .addNumberList("sample_names", 1 ,2 ,3, 4)
                                                       .build();
    
// put to dynamo db table
documentTable.documentFromBuilder(documentFromBuilder);

// retrieving a document from dynamo db and updating some attributes
EnhancedDocument documentTableItem = documentTable.getItem(hashKeyDocument);
// using toBuilder to make a copy of the retrieved item and then modifying the key attribute
EnhancedDocument changedValue = documentTableItem.toBuilder().addString("key-to-change", "changedValue").build();
// put to dynamo db table
documentTable.putItem(changedValue);
~~~


#### EnhancedDocument 的 Attribute converter providers

将提供 builder 方法为 `EnhancedDocument` 添加 Attribute 转换器。`EnhancedDocument` 上 attribute converter 字段默认为 null。

问：从 DynamoDB 取回的 `EnhancedDocument` 使用哪些 converter providers？<br>
答：对 SDK get/scan/query 返回的 `EnhancedDocument`，默认分配 `DefaultAttributeConverterProviders`。若用户在建表时提供了 attribute converter providers，则使用用户提供的转换器。

问：用户自行创建的 `EnhancedDocument` 使用哪些 converter providers？<br>
答：使用用户在 `EnhancedDocument` builder 中提供的转换器。若未提供，则在尝试读取属性值时报错。因此，若用户后续需要读取属性值，创建 `EnhancedDocument` 时应始终提供 `defaultConverterProviders`。


#### EnhancedDocument 的 Getter 与 Setter

问：`EnhancedDocument` 提供哪些 getter API？<br>
答：
 1. 与 V1 类似的类型专用 getter，如 `getString()`、`getNumber`、`getMap()`。
 2. 适用于任意 `EnhancedType` 的泛型 getter API。
 3. 带 ConverterProviders 的 getter（原文 "in ares" 疑为笔误）

问：`EnhancedDocument` 提供哪些 setter API？<br>
答：以下 builder API：
1. 与 V1 类似的类型专用 builder，如 `addString()`、`addNumber`、`addMap()`。
2. 适用于任意 `EnhancedType` 的泛型 builder，如 `add(value, EnhancedType)`。
3. 配合自定义 converter providers 的自定义类 builder。

## 附录 B：替代方案

### 设计替代：使用 Sdk-core 现有 Document API
该思路为 `software.amazon.awssdk.core.document.Document` 增补 API，以更好读写开放内容。

#### 从 DynamoDB 读取扁平结构
~~~java
Document document = table.getItem(Key.builder().partitionValue("0").build());
// Current Document API
String id = document.asMap().get("id").asString();
Instant time = Instant.parse(document.asMap().get("time").asString());

// Document Converters API
String id = document.get("id", String.class);
Instant time = document.get("time", Instant.class);

// Document JSON API
String json = document.toJsonString();

~~~

**决策**

该方案被否决，因其未利用现有 converter providers。需将 key–AttributeValue 映射转为 Document，并要实现新的 jsonNode 转换器。
