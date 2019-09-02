package com.demo.service.impl;

import com.demo.entities.User;
import com.demo.dao.UserDao;
import com.demo.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author laojie
 * @since 2019-08-29
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

}
