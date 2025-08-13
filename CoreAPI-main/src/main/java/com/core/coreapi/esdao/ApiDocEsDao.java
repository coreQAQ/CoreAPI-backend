package com.core.coreapi.esdao;

import com.core.coreapi.model.entity.ApiDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 文档 ES 操作
 */
@Repository
public interface ApiDocEsDao extends ElasticsearchRepository<ApiDoc, Long> {

}