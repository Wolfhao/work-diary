package com.workdiary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workdiary.entity.User;
import com.workdiary.mapper.UserMapper;
import com.workdiary.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}
