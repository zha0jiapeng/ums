# 属性键配置 dataType 字段反序列化说明

## 功能说明

在返回 `UmsPropertyKeys` 数据时，会自动将 `dataType` 字段（Integer 类型）反序列化为 `dataTypeInfo` 对象，包含完整的枚举信息。

## 数据类型枚举值对照表

| 值 | code | description | 用途 |
|----|------|-------------|------|
| 0  | string | 字符串 | 文本数据 |
| 1  | integer | 整数 | 整数数值 |
| 2  | float | 单精度浮点数 | 小数（单精度） |
| 3  | double | 双精度浮点数 | 小数（双精度） |
| 4  | long | 长整数 | 大整数 |
| 5  | boolean | 布尔值 | true/false |
| 6  | json | JSON对象 | JSON 格式数据 |
| 7  | binary | 二进制数据 | 文件、图片等 |
| 8  | datetime | 日期时间 | 时间戳或日期 |
| 9  | array | 数组 | 数组数据 |
| 10 | unknown | 未知类型 | 未定义类型 |

## API 返回示例

### 单个记录查询

**请求：**
```
GET /system/property-keys/1
```

**响应：**
```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "key": "username",
    "dataType": 0,
    "dataTypeInfo": {
      "value": 0,
      "code": "string",
      "description": "字符串"
    },
    "description": "用户名",
    "size": 255,
    "scope": 0
  }
}
```

### 列表查询

**请求：**
```
GET /system/property-keys/list?pageNum=1&pageSize=10
```

**响应：**
```json
{
  "code": 0,
  "msg": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "key": "username",
        "dataType": 0,
        "dataTypeInfo": {
          "value": 0,
          "code": "string",
          "description": "字符串"
        },
        "description": "用户名",
        "size": 255,
        "scope": 0
      },
      {
        "id": 2,
        "key": "age",
        "dataType": 1,
        "dataTypeInfo": {
          "value": 1,
          "code": "integer",
          "description": "整数"
        },
        "description": "年龄",
        "size": 11,
        "scope": 0
      },
      {
        "id": 3,
        "key": "avatar",
        "dataType": 7,
        "dataTypeInfo": {
          "value": 7,
          "code": "binary",
          "description": "二进制数据"
        },
        "description": "头像",
        "size": 1048576,
        "scope": 0
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

## 实现细节

### 1. 实体类增强

在 `UmsPropertyKeys` 实体类中：
- 保留原有的 `dataType` 字段（Integer 类型）用于数据库存储
- 新增 `dataTypeInfo` 字段（非数据库字段）用于返回枚举详情
- 提供 `fillDataTypeInfo()` 方法自动填充枚举信息

### 2. Service 层自动填充

所有查询方法都会自动调用 `fillDataTypeInfo()` 填充数据：
- `getById()` - 单条查询
- `getByKey()` - 按 key 查询
- `list()` - 列表查询
- `getAllKeysMap()` - 获取全部配置

### 3. Controller 层补充

分页查询时，在 Controller 中手动调用 `fillDataTypeInfo()` 确保数据完整性。

## 前端使用建议

### 显示数据类型

```javascript
// 使用 dataTypeInfo.description 显示中文描述
const dataTypeName = propertyKey.dataTypeInfo.description; // "字符串"

// 使用 dataTypeInfo.code 作为技术标识
const dataTypeCode = propertyKey.dataTypeInfo.code; // "string"
```

### 表单选择器

```vue
<template>
  <el-select v-model="form.dataType" placeholder="请选择数据类型">
    <el-option
      v-for="item in dataTypeOptions"
      :key="item.value"
      :label="item.description"
      :value="item.value"
    >
      <span>{{ item.description }}</span>
      <span style="color: #8492a6; font-size: 13px">{{ item.code }}</span>
    </el-option>
  </el-select>
</template>

<script>
export default {
  data() {
    return {
      dataTypeOptions: [
        { value: 0, code: 'string', description: '字符串' },
        { value: 1, code: 'integer', description: '整数' },
        { value: 2, code: 'float', description: '单精度浮点数' },
        { value: 3, code: 'double', description: '双精度浮点数' },
        { value: 4, code: 'long', description: '长整数' },
        { value: 5, code: 'boolean', description: '布尔值' },
        { value: 6, code: 'json', description: 'JSON对象' },
        { value: 7, code: 'binary', description: '二进制数据' },
        { value: 8, code: 'datetime', description: '日期时间' },
        { value: 9, code: 'array', description: '数组' },
        { value: 10, code: 'unknown', description: '未知类型' }
      ]
    }
  }
}
</script>
```

### 表格显示

```vue
<el-table-column label="数据类型" prop="dataTypeInfo.description">
  <template slot-scope="scope">
    <el-tag :type="getDataTypeTagType(scope.row.dataType)">
      {{ scope.row.dataTypeInfo.description }}
    </el-tag>
  </template>
</el-table-column>
```

## 数据库插入示例

当插入新的属性键配置时，使用数据类型枚举值：

```sql
-- 插入字符串类型的属性键
INSERT INTO ums_property_keys (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('username', 0, '用户名', 255, 0);

-- 插入整数类型的属性键
INSERT INTO ums_property_keys (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('age', 1, '年龄', 11, 0);

-- 插入二进制类型的属性键（用于存储头像）
INSERT INTO ums_property_keys (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('avatar', 7, '头像', 1048576, 0);

-- 插入 JSON 类型的属性键
INSERT INTO ums_property_keys (`key`, `data_type`, `description`, `size`, `scope`)
VALUES ('settings', 6, '用户设置', 10240, 0);
```

## 注意事项

1. **dataType 和 dataTypeInfo 的关系**
   - `dataType` 是数据库存储的整数值
   - `dataTypeInfo` 是系统自动填充的枚举详情，不存储在数据库中
   - 两者的值保持同步，`dataTypeInfo.value` 等于 `dataType`

2. **前端提交数据时**
   - 只需要提交 `dataType` 字段（Integer 值）
   - 不需要提交 `dataTypeInfo` 字段

3. **API 响应**
   - 所有查询接口都会自动返回 `dataTypeInfo`
   - 可以根据 `dataTypeInfo` 进行前端展示和业务逻辑判断

4. **缓存实时刷新**
   - 系统会每 30 秒自动刷新配置缓存
   - 首次使用新 key 时会立即从数据库查询
   - 无需手动调用刷新接口
