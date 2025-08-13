## 接口名称：${name}

**描述：** ${description}

**域名：** `${host}`

**路径：** `${path}`

**请求方法：** `${method}`

**响应格式：** `${responseFormat}`

---

<#if requestParams?size gt 0>
### 请求参数：

| 参数名       | 类型   | 位置   | 是否必选 | 描述         |
|--------------|--------|--------|----------|--------------|
<#-- 遍历请求参数列表 -->
<#list requestParams as param>
| ${param.name} | ${param.type} | ${param.position} | <#if param.required == 1>是<#else>否</#if> | ${param.description} |
</#list>

---
</#if>

<#if responseParams?size gt 0 >
### 响应参数：

| 参数名       | 类型   | 描述         |
|--------------|--------|--------------|
<#-- 遍历响应参数列表 -->
<#list responseParams as param>
| ${param.name} | ${param.type} | ${param.description} |
</#list>

---
</#if>>

### 请求示例：

```json
${requestExample}
```

### 响应示例：

```json
${responseExample}
```