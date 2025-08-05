package com.global.ums.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.global.ums.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
} 