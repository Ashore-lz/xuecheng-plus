package com.xuecheng.content.feignclient;

import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/19 10:12
 */
@Slf4j
@Component
public class MediaServiceClientFallbackFactory implements FallbackFactory<MediaServiceClient> {

    //使用FallbackFactory可以获取异常信息
    @Override
    public MediaServiceClient create(Throwable throwable) {

        return new MediaServiceClient(){

            @Override
            public String upload(MultipartFile filedata, String folder, String objectName) {
                throwable.printStackTrace();
                //降级方法
                log.debug("调用媒资管理服务上传文件时发生熔断，异常信息:{}",throwable.getMessage());
                return null;
            }
        };
    }
}
