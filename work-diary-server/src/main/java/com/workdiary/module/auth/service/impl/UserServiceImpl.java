package com.workdiary.module.auth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workdiary.module.auth.mapper.UserMapper;
import com.workdiary.module.auth.service.UserService;
import com.workdiary.shared.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
