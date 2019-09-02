package com.demo.controller;


import com.demo.Utils.MD5Util;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.stereotype.Controller;

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
@RequestMapping("/user")
public class UserController {

    public void test(){
        MD5Util.getMD5String("");
    }

}
