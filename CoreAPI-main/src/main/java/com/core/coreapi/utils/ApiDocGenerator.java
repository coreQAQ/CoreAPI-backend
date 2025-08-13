package com.core.coreapi.utils;

import com.core.coreapi.common.CommonUtils;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.model.dto.doc.Document;
import com.core.coreapi.model.dto.doc.Param;
import com.core.coreapi.shared.common.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 将单个 Path + Method 的 OpenAPI 片段转换为自定义 Document
 */
@Slf4j
public class ApiDocGenerator {

    /** 单例 mapper，线程安全 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** #region ——只读字段—— */
    private final JsonNode schemas;
    private final JsonNode operation;
    private final String name;
    private final String path;
    private final String method;
    /** #endregion */

    public ApiDocGenerator(String name, String path, String method, String openapi) {
        // 解析 OpenAPI JSON
        JsonNode openapiNode;
        try {
            openapiNode = MAPPER.readTree(openapi);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OpenAPI JSON 解析失败");
        }
        this.name = name;
        this.path   = Objects.requireNonNull(path);
        this.method = Objects.requireNonNull(method).toLowerCase(Locale.ROOT);

        String pathWithoutBasePath = path.replaceFirst("(?i)/api(?![a-zA-Z0-9_])", "");
        this.operation = openapiNode.at(String.format("/paths/%s/%s",
                CommonUtils.escapeJsonPointer(pathWithoutBasePath), this.method));
        if (operation.isMissingNode()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "生成文档时未找到此路径的接口");
        }

        this.schemas = openapiNode.at("/components/schemas");
        if (schemas.isMissingNode()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "OpenAPI 不合规：缺少 components.schemas");
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Public API                                                            */
    /* ---------------------------------------------------------------------- */

    public Document build() {

        // 1. 请求 & 响应参数
        List<Param> reqParams  = buildRequestParams();
        List<Param> respParams = buildResponseParams();

        // 2. 构造 document
        Document doc = new Document();
        if (StringUtils.isBlank(name)) {
            doc.setName(operation.path("operationId").asText());
        } else {
            doc.setName(name);
        }
        doc.setPath(path);
        doc.setMethod(method);
        doc.setReqParams(reqParams);
        doc.setRespParams(respParams);

        // 3. 填充示例（三个字段都保证不为空，空用 "" 占位）
        fillExamples(doc);

        return doc;
    }

    /* ---------------------------------------------------------------------- */
    /*  Internal helpers                                                      */
    /* ---------------------------------------------------------------------- */

    /** 解析 parameters / requestBody */
    private List<Param> buildRequestParams() {
        List<Param> params = new ArrayList<>();

        /* ---- 3.1 query / header / path 等 ---- */
        JsonNode parametersNode = operation.path("parameters");
        if (!parametersNode.isMissingNode()) {
            parametersNode.forEach(p -> params.add(convertParameter(p)));
        }

        /* ---- 3.2 body ---- */
        JsonNode bodySchema = operation.at("/requestBody/content/application~1json/schema");
        if (!bodySchema.isMissingNode()) {
            Param body = handleSchema(bodySchema);
            body.setPosition("body");
            body.setRequired(true);
            params.add(body);
        }
        return params;
    }

    /** 解析 200 响应，仅演示主 happy-path，可按需扩展其它 code */
    private List<Param> buildResponseParams() {
        JsonNode schema = operation.at("/responses/200/content/*~1*/schema");
        return schema.isMissingNode() ? Collections.emptyList()
                                      : Collections.singletonList(handleSchema(schema));
    }

    /** 单个 parameter -> Param */
    private Param convertParameter(JsonNode p) {
        Param param = new Param();
        param.setName(p.path("name").asText());
        param.setPosition(p.path("in").asText());
        param.setRequired(p.path("required").asBoolean(false));
        param.setDescription(p.path("description").asText());

        JsonNode schema = p.path("schema");
        if ("array".equals(schema.path("type").asText())) {
            param.setType(schema.path("items").path("type").asText() + "[]");
        } else {
            param.setType(schema.path("type").asText());
        }
        param.setExample(MAPPER.convertValue(p.path("example"), Object.class));
        return param;
    }

    /** 递归处理 schema，兼容 基本类型 / 对象 / 数组 / 对象数组 */
    private Param handleSchema(JsonNode schema) {

        Param param = new Param();

        if (schema.has("type")) {
            String type = schema.path("type").asText();

            switch (type) {
                case "array":
                    JsonNode items = schema.path("items");
                    return buildArrayParam(items, schema);
                case "object":
                    // 这个分支一般不会出现
                    break;
                default:
                    param.setName(CommonUtils.lowercaseFirstLetter(type));
                    param.setType(type);
                    param.setDescription(schema.path("description").asText());
                    param.setExample(MAPPER.convertValue(schema.path("example"), Object.class));
                    return param;
            }
        }

        // $ref 对象
        if (schema.has("$ref")) {
            String schemaName = CommonUtils.getSchemaName(schema.get("$ref").asText());
            param.setName(CommonUtils.lowercaseFirstLetter(schemaName));
            param.setType(schemaName);
            param.setDescription(schemas.path(schemaName).path("description").asText());
            param.setChildren(buildChildren(schemaName));
        }
        return param;
    }

    /** 数组或对象数组 */
    private Param buildArrayParam(JsonNode items, JsonNode parentSchema) {
        Param param = new Param();
        param.setDescription(parentSchema.path("description").asText());

        // name 默认使用 type 或 $ref 加个 s
        if (items.has("$ref")) {                       // 对象数组
            String childSchemaName = CommonUtils.getSchemaName(items.get("$ref").asText());
            param.setName(CommonUtils.lowercaseFirstLetter(childSchemaName) + "s");
            param.setType(childSchemaName + "[]");
            param.setChildren(buildChildren(childSchemaName));
        } else {                                       // 基础类型数组
            String itemType = items.path("type").asText();
            param.setName(CommonUtils.lowercaseFirstLetter(itemType) + "s");
            param.setType(itemType + "[]");
            param.setExample(MAPPER.convertValue(parentSchema.get("example"), Object.class));
        }
        return param;
    }

    /** 递归构造字段 children */
    private List<Param> buildChildren(String schemaName) {
        JsonNode schema = schemas.path(schemaName);
        if (schema.isMissingNode()) {
            log.warn("未找到子 schema: {}", schemaName);
            return Collections.emptyList();
        }

        JsonNode props = schema.path("properties");
        Iterable<Map.Entry<String, JsonNode>> iterable = () -> props.fields();

        return StreamSupport.stream(iterable.spliterator(), false)
                .map(entry -> {
                    String fieldName = entry.getKey();
                    JsonNode fieldVal = entry.getValue();

                    Param child = new Param();
                    child.setName(fieldName);
                    child.setDescription(fieldVal.path("description").asText());
                    child.setRequired(!fieldVal.path("required").isMissingNode()); // 若需精准，可读取父 schema 的 required 数组

                    if ("array".equals(fieldVal.path("type").asText())) {            // 数组
                        JsonNode items = fieldVal.path("items");
                        Param param = buildArrayParam(items, fieldVal);
                        // 重设 name
                        param.setName(fieldName);
                        return param;
                    } else if (fieldVal.has("$ref")) {                               // 对象
                        String refName = CommonUtils.getSchemaName(fieldVal.get("$ref").asText());
                        child.setType(refName);
                        child.setDescription(schemas.path(refName).path("description").asText());
                        child.setChildren(buildChildren(refName));
                    } else {                                                         // 基础
                        child.setType(fieldVal.path("type").asText());
                        child.setExample(MAPPER.convertValue(fieldVal.path("example"), Object.class));
                    }
                    return child;
                })
                .collect(Collectors.toList());
    }

    /* -------------------------- example builder -------------------------- */

    private void fillExamples(Document doc) {
        // 1.queryExample
        String queryParams = doc.getReqParams()
                .stream()
                .filter(p -> "query".equals(p.getPosition()))
                .map(p -> p.getName() + "=" + p.getExample())
                .collect(Collectors.joining("&", "", ""));
        doc.setQueryExample(queryParams.isEmpty() ? "" : queryParams);

        // 2. bodyExample
        JsonNode bodySchema = operation.at("/requestBody/content/application~1json/schema");
        if (!bodySchema.isMissingNode()) {
            doc.setReqBodyExample(buildExampleJson(bodySchema));
        } else {
            doc.setReqBodyExample("");
        }

        // 3. respExample
        JsonNode respSchema = operation.at("/responses/200/content/*~1*/schema");
        if (!respSchema.isMissingNode()) {
            doc.setRespExample(buildExampleJson(respSchema));
        } else {
            doc.setRespExample("");
        }
    }

    /**
     * 根据 schema 生成示例 JSON 字符串
     */
    private String buildExampleJson(JsonNode schema) {
        try {
            if (schema.has("example")) {
                // 基础类型可直接 toString
                return MAPPER.writeValueAsString(schema.get("example"));
            }

            // 对象 / 数组
            String schemaName;
            if ("array".equals(schema.path("type").asText())) {
                JsonNode items = schema.path("items");
                schemaName = items.has("$ref")
                        ? CommonUtils.getSchemaName(items.get("$ref").asText())
                        : null;
                ArrayNode array = MAPPER.createArrayNode();
                array.add(schemaName == null
                        ? items.get("example")
                        : MAPPER.readTree(buildExample(schemaName)));
                return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(array);
            } else if (schema.has("$ref")) {
                schemaName = CommonUtils.getSchemaName(schema.get("$ref").asText());
                return buildExample(schemaName);
            }

            // 兜底
            return "{}";
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "构建示例失败");
        }
    }

    /**
     * 递归构建对象示例
     */
    private String buildExample(String schemaName) throws JsonProcessingException {
        JsonNode schema = schemas.path(schemaName);
        ObjectNode exampleNode = MAPPER.createObjectNode();

        // properties
        schema.path("properties").fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode val = entry.getValue();

            if (val.has("example")) {                                  // 基础
                exampleNode.set(key, val.get("example"));
            } else if (val.has("$ref")) {                              // 对象
                String child = CommonUtils.getSchemaName(val.get("$ref").asText());
                try {
                    exampleNode.set(key, MAPPER.readTree(buildExample(child)));
                } catch (JsonProcessingException e) {
                    log.warn("build example fail for {}", child, e);
                }
            } else if ("array".equals(val.path("type").asText())) {    // 数组 / 对象数组
                ArrayNode arr = MAPPER.createArrayNode();
                JsonNode items = val.path("items");
                if (items.has("$ref")) {
                    String child = CommonUtils.getSchemaName(items.get("$ref").asText());
                    try {
                        arr.add(MAPPER.readTree(buildExample(child)));
                    } catch (JsonProcessingException e) {
                        log.warn("build example fail for {}", child, e);
                    }
                } else if (items.has("example")) {
                    arr.add(items.get("example"));
                }
                exampleNode.set(key, arr);
            }
        });
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(exampleNode);
    }
}