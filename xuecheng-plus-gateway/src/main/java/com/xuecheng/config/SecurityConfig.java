package com.xuecheng.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @description 安全配置类
 * @author Mr.M
 * @date 2022/9/27 12:07
 * @version 1.0
 */
 @EnableWebFluxSecurity
 @Configuration
 public class SecurityConfig {


  //安全拦截配置
  @Bean
  public SecurityWebFilterChain webFluxSecurityFilterChain(ServerHttpSecurity http) {

   return http.authorizeExchange()
           .pathMatchers("/**").permitAll()
           .anyExchange().authenticated()
           .and().csrf().disable().build();
  }


 }
