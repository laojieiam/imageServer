package com.demo.controller;


import com.demo.Utils.MD5Util;
import org.apache.catalina.security.SecurityUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.MessageDigest;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author laojie
 * @since 2019-08-29
 */
@Controller
public class UserController {

    @RequestMapping("/")
    public String index(){
        if(SecurityUtils.getSubject().isAuthenticated())
            return "redirect:/images/imageList";
        return "index";
    }

    @RequestMapping("/user/logout")
    public String logout(){
//        SecurityUtils.getSubject().logout();
        return "redirect:/";
    }

    @RequestMapping("/user/login")
    public String login(@RequestParam("username")String username,
                        @RequestParam("password")String password){

        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token=new UsernamePasswordToken(username,password);
        try {
            subject.login(token);
        }catch (Exception e){
//            e.printStackTrace();
            return "redirect:/images/imageList";
        }
        return "redirect:/images/imageList";
    }

}
