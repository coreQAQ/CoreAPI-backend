### 请求示例
<#assign method = doc.method?upper_case!'请求方法未定义'>
<#assign path = doc.path!'接口路径未定义'>
<#assign query = doc.queryExample?has_content?string('?' + doc.queryExample, '')>
<#assign body = doc.reqBodyExample?has_content?string('\n' + doc.reqBodyExample, '')>
``` json
${method} ${path}${query}${body}
```
<#if doc.reqParams?has_content>
    <#compress>
        ### 请求参数
        | 参数名 | 类型 | 必填 | 描述 | 示例 |
        |--------|------|------|------|------|
        <@renderParams params=doc.reqParams parentPath=""/>
    </#compress>
</#if>

<#if doc.respParams?has_content>
    <#compress>
        ### 响应参数
        | 参数名 | 类型 | 必填 | 描述 | 示例 |
        |--------|------|------|------|------|
        <@renderParams params=doc.respParams[0].children parentPath=""/>
    </#compress>
</#if>

### 响应示例
```json
${doc.respExample!''}
```

<#macro renderParams params parentPath>
<#-- 渲染无 children 的字段 -->
    <#list params as p>
        <#assign fullName = (parentPath?has_content?string(parentPath + '.', '')) + p.name>
        | ${fullName} | ${p.type!'object'} | ${(p.required!false)?string('是','否')} | ${p.description!''} | ${showExample(p.example!'')} |
    </#list>

<#-- 渲染有 children 的字段的说明与子字段表格 -->
    <#list params as p>
        <#if p.children?? && p.children?has_content>
            <#assign fullName = (parentPath?has_content?string(parentPath + '.', '')) + p.name>

            #### ${fullName} 字段说明

            | 参数名 | 类型 | 必填 | 描述 | 示例 |
            |--------|------|------|------|------|

            <@renderParams params=p.children parentPath=fullName />
        </#if>
    </#list>
</#macro>

<#-- 放在模板顶部 (或公共宏库) -->
<#function showExample val>
<#-- null 直接返回空串 -->
    <#if !val??>
        <#return "">
    </#if>

<#-- ① 数组 / List / Sequence → 转成 JSON 风格 -->
    <#if val?is_sequence>
    <#-- 把每个元素格式化后 join -->
        <#assign parts = []>
        <#list val as it>
        <#-- 字符串加双引号；布尔用 ?c；数字原样 -->
            <#if it?is_string>
                <#assign parts += ['"' + it?js_string + '"']>
            <#elseif it?is_boolean>
                <#assign parts += [it?c]>
            <#else>
                <#assign parts += [it]>
            </#if>
        </#list>
        <#return "[" + parts?join(", ") + "]">
    </#if>

<#-- ② 单个布尔值 -->
    <#if val?is_boolean>
        <#return val?c>
    </#if>

<#-- ③ 其它（数字、字符串等）直接转字符串 -->
    <#return val?string>
</#function>