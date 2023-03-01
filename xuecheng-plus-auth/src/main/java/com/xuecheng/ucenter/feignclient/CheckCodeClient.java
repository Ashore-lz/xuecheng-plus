package com.xuecheng.ucenter.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/20 15:50
 * @version 1.0
 */
 @FeignClient(value = "checkcode",fallbackFactory = CheckCodeClientFactory.class)
 @RequestMapping("/checkcode")
public interface CheckCodeClient {

 @PostMapping(value = "/verify")
 public Boolean verify(@RequestParam("key") String key,@RequestParam("code") String code);

}
