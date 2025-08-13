package com.core.coreapi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreapi.model.entity.PhoneLocation;
import com.core.coreapi.mapper.PhoneLocationMapper;
import com.core.coreapi.service.PhoneLocationService;

import org.springframework.stereotype.Service;

/**
 * 手机号归属地服务实现
*/
@Service
public class PhoneLocationServiceImpl extends ServiceImpl<PhoneLocationMapper, PhoneLocation>
    implements PhoneLocationService {

}




