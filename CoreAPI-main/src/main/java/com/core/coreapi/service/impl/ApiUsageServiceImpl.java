package com.core.coreapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreapi.shared.entity.ApiUsage;
import com.core.coreapi.service.ApiUsageService;
import com.core.coreapi.mapper.ApiUsageMapper;
import org.springframework.stereotype.Service;

/**
 * 调用统计服务实现
 *
*/
@Service
public class ApiUsageServiceImpl extends ServiceImpl<ApiUsageMapper, ApiUsage>
    implements ApiUsageService{

}




