package com.core.coreapi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.core.coreapi.constant.CommonConstant;
import com.core.coreapi.esdao.ApiDocEsDao;
import com.core.coreapi.exception.BusinessException;
import com.core.coreapi.exception.ThrowUtils;
import com.core.coreapi.model.dto.apidoc.ApiDocQueryRequest;
import com.core.coreapi.model.dto.doc.Document;
import com.core.coreapi.model.entity.ApiDoc;
import com.core.coreapi.service.ApiDocService;
import com.core.coreapi.service.ApiInfoService;
import com.core.coreapi.shared.common.ErrorCode;
import com.core.coreapi.shared.entity.ApiInfo;
import com.core.coreapi.utils.ApiDocGenerator;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ApiDocServiceImpl implements ApiDocService {

    @Resource
    private OkHttpClient httpClient;

    @Resource
    private Configuration freemarkerConfig;

    @Resource
    private ApiDocEsDao apiDocEsDao;

    @Resource
    private ElasticsearchRestTemplate template;

    @Resource
    private ApiInfoService apiInfoService;
    @Autowired
    private StringHttpMessageConverter stringHttpMessageConverter;

    /**
     * 校验数据
     *
     * @param apiDoc
     * @param add    对创建的数据进行校验
     */
    @Override
    public void validApiDoc(ApiDoc apiDoc, boolean add) {
        ThrowUtils.throwIf(apiDoc == null, ErrorCode.PARAMS_ERROR);
        Long id = apiDoc.getId();
        Long userId = apiDoc.getUserId();
        String title = apiDoc.getTitle();
        String description = apiDoc.getDescription();
        String content = apiDoc.getContent();
        Integer priority = apiDoc.getPriority();
        // 创建数据时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(title) || priority == null, ErrorCode.PARAMS_ERROR);
        }
        // 修改数据时，有参数则校验
        if (StringUtils.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
        }
    }

    @Override
    public ApiDoc getById(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        // 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("id", id))
                .must(QueryBuilders.termQuery("isDelete", 0));
        // 执行查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .build();
        Optional<SearchHit<ApiDoc>> optional = template.search(searchQuery, ApiDoc.class)
                .getSearchHits()
                .stream()
                .findFirst();

        ThrowUtils.throwIf(optional.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        return optional.get().getContent();
    }

    @Override
    public Long add(ApiDoc apiDoc) {
        ThrowUtils.throwIf(apiDoc == null, ErrorCode.PARAMS_ERROR);
        // 1.填充默认值
        if (apiDoc.getId() == null)
            // 未指定id的话自动填充
            apiDoc.setId(IdUtil.getSnowflakeNextId());
        apiDoc.setCreateTime(new Date());
        apiDoc.setUpdateTime(new Date());
        apiDoc.setIsDelete(0);
        // 2.写入ES
        try {
            apiDocEsDao.save(apiDoc);
        } catch (ElasticsearchException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加文档失败");
        }
        return apiDoc.getId();
    }

    @Override
    public boolean updateById(ApiDoc apiDoc) {
        ThrowUtils.throwIf(apiDoc == null || apiDoc.getId() == null || apiDoc.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 1.校验数据
        validApiDoc(apiDoc, false);
        // 2.判断是否存在
        Optional<ApiDoc> optional = apiDocEsDao.findById(apiDoc.getId());
        ThrowUtils.throwIf(optional.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "文档不存在");
        // 3.填充默认值
        ApiDoc oldApiDoc = optional.get();
        BeanUtil.copyProperties(apiDoc, oldApiDoc, CopyOptions.create().setIgnoreNullValue(true));
        oldApiDoc.setUpdateTime(new Date());
        // 4.写入ES
        try {
            apiDocEsDao.save(oldApiDoc);
        } catch (ElasticsearchException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新文档失败");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        // 1.判断是否存在
        Optional<ApiDoc> optional = apiDocEsDao.findById(id);
        ThrowUtils.throwIf(optional.isEmpty(), ErrorCode.NOT_FOUND_ERROR, "文档不存在");

        // 2.接口和文档解绑
        ApiDoc apiDoc = optional.get();
        // 没规定api_info表docId不可重复，先用list
        List<ApiInfo> apiInfoList = apiInfoService.list(new QueryWrapper<ApiInfo>().eq("docId", apiDoc.getId()));
        for (ApiInfo apiInfo : apiInfoList) {
            apiInfo.setDocId(-1L); // -1表示未绑定
        }
        apiInfoService.updateBatchById(apiInfoList);

        // 3.更新es
        apiDoc.setIsDelete(1);
        apiDoc.setUpdateTime(new Date());
        try {
            apiDocEsDao.save(apiDoc);
        } catch (ElasticsearchException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除文档失败");
        }

        return true;
    }

    @Override
    public List<ApiDoc> listApiDocBrief() {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("isDelete", 0));
        // 排除content
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withSourceFilter(new FetchSourceFilter(
                        null, new String[]{"content"}
                ))
                .withSort(Sort.by(Sort.Direction.DESC, "priority"))
                .build();

        SearchHits<ApiDoc> hits = template.search(searchQuery, ApiDoc.class);
        return hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public Document genDoc(String url, String name, String path, String method) {
        if (StringUtils.isAnyBlank(url, name, path, method)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成文档失败，缺少参数");
        }
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            // 1.获取 OpenAPI 文档
            if (response.body() == null)
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "获取 OpenAPI 失败");
            String openapi = response.body().string();
            Document doc = new ApiDocGenerator(name, path, method, openapi).build();
            // 2.生成文档
            HashMap<String, Object> dataModel = new HashMap<>();
            dataModel.put("doc", doc);
            Template template = freemarkerConfig.getTemplate("ApiDocument.ftl");

            try (Writer out = new StringWriter()) {
                template.process(dataModel, out);
                doc.setContent(out.toString());
            }

            return doc;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "获取 OpenAPI 失败");
        } catch (TemplateException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成文档失败");
        }
    }

    @Override
    public Page<ApiDoc> searchFromEs(ApiDocQueryRequest apiDocQueryRequest) {
        Long id = apiDocQueryRequest.getId();
        String title = apiDocQueryRequest.getTitle();
        String description = apiDocQueryRequest.getDescription();
        String content = apiDocQueryRequest.getContent();
        String searchText = apiDocQueryRequest.getSearchText();
        String sortField = apiDocQueryRequest.getSortField();
        Sort.Direction direction = CommonConstant.SORT_ORDER_ASC.equals(apiDocQueryRequest.getSortOrder()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int current = apiDocQueryRequest.getCurrent();
        int pageSize = apiDocQueryRequest.getPageSize();

        // 构建查询条件
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("isDelete", 0));
        if (StringUtils.isNotBlank(searchText))
            boolQuery.must(QueryBuilders.multiMatchQuery(searchText, "title", "content"));
        if (id != null)
            boolQuery.must(QueryBuilders.termQuery("id", id));
        if (StringUtils.isNotBlank(title))
            boolQuery.must(QueryBuilders.matchQuery("title", title));
        if (StringUtils.isNotBlank(description))
            boolQuery.must(QueryBuilders.matchQuery("description", description));
        if (StringUtils.isNotBlank(content))
            boolQuery.must(QueryBuilders.matchQuery("content", content));

        // SearchQueryBuilder
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(current - 1, pageSize));

        if (StringUtils.isNotBlank(searchText))
            searchQueryBuilder.withHighlightBuilder(new HighlightBuilder()
                    .field("title")
                    .field("content")
                    .numOfFragments(6));

        if (StringUtils.isNotBlank(sortField)) {
            searchQueryBuilder.withSort(Sort.by(direction, sortField));
        }

        NativeSearchQuery searchQuery = searchQueryBuilder.build();

        SearchHits<ApiDoc> hits = template.search(searchQuery, ApiDoc.class);
        List<ApiDoc> apiDocList = hits.getSearchHits().stream()
                .map(searchHit -> {
                    ApiDoc hit = searchHit.getContent();
                    ApiDoc apiDoc = new ApiDoc();
                    apiDoc.setId(hit.getId());
                    apiDoc.setUserId(hit.getUserId());
                    apiDoc.setDescription(hit.getDescription());
                    apiDoc.setPriority(hit.getPriority());
                    apiDoc.setCreateTime(hit.getCreateTime());
                    apiDoc.setUpdateTime(hit.getUpdateTime());
                    apiDoc.setIsDelete(0);

                    if (searchHit.getHighlightField("title").isEmpty())
                        apiDoc.setTitle(hit.getTitle());
                    else
                        apiDoc.setTitle(searchHit.getHighlightField("title").get(0));
                    // 文档内容若无匹配则仅返回前 50 个字
                    if (searchHit.getHighlightField("content").isEmpty())
                        apiDoc.setContent(hit.getContent().substring(0, Math.min(hit.getContent().length(), 50)));
                    else {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (String str : searchHit.getHighlightField("content")) {
                            stringBuilder.append(str);
                        }
                        apiDoc.setContent(stringBuilder.toString());
                    }

                    return apiDoc;
                })
                .collect(Collectors.toList());

        return new Page<ApiDoc>(current, pageSize).setRecords(apiDocList).setTotal(apiDocList.size());
    }

}



