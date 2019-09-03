package com.demo.entities;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

public class UserRealm extends AuthorizingRealm {

    @Autowired
    private UserService userService;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        //不准备做权限分配这些
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {

        UsernamePasswordToken token=(UsernamePasswordToken)authenticationToken;
        User user = userService.getOne(new QueryWrapper<User>().eq("username", token.getUsername().toString()));
        if(null==user)return null;

        return new SimpleAuthenticationInfo("",user.getPassword(),"");
    }
}
