package com.xuecheng.learning.feignclient;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.content.model.po.CoursePublish;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @description 媒资管理服务远程接口
 * @author Mr.M
 * @date 2022/9/20 20:29
 * @version 1.0
 */
 @FeignClient(value = "media-api",fallbackFactory = MediaServiceClientFallbackFactory.class)
 @RequestMapping("/media")
public interface MediaServiceClient {

 @GetMapping("/open/preview/{mediaId}")
 public RestResponse<String> getPlayUrlByMediaId(@PathVariable("mediaId") String mediaId);

 }
