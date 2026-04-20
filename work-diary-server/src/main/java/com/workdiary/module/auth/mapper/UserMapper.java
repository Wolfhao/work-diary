package com.workdiary.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workdiary.shared.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
