package com.workdiary.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 与 Sa-Token 路由拦截配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定一条 match 规则
            SaRouter
                    .match("/**") // 拦截的 path 列表，可以写多个
                    .notMatch("/login", "/wx/login", "/swagger-ui/**", "/v3/api-docs/**", "/doc.html", "/webjars/**") // 排除掉的
                                                                                                                      // path
                                                                                                                      // 列表
                    .check(r -> StpUtil.checkLogin()); // 要执行的校验动作，可以写完整的 lambda 表达式

        })).addPathPatterns("/**");
    }
}
