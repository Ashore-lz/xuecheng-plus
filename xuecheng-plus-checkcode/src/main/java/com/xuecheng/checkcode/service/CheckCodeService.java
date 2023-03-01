package com.xuecheng.checkcode.service;

import com.xuecheng.checkcode.model.CheckCodeParamsDto;
import com.xuecheng.checkcode.model.CheckCodeResultDto;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author Mr.M
 * @version 1.0
 * @description 验证码接口
 * @date 2022/9/29 15:59
 */
public interface CheckCodeService {


    /**
     * @description 生成验证码
     * @param checkCodeParamsDto 生成验证码参数
     * @return com.xuecheng.checkcode.model.CheckCodeResultDto 验证码结果
     * @author Mr.M
     * @date 2022/9/29 18:21
    */
     CheckCodeResultDto generate(CheckCodeParamsDto checkCodeParamsDto);

     /**
      * @description 校验验证码
      * @param key
      * @param code
      * @return boolean
      * @author Mr.M
      * @date 2022/9/29 18:46
     */
    public boolean verify(String key, String code);


    /**
     * @description 验证码生成器
     * @author Mr.M
     * @date 2022/9/29 16:34
    */
    public interface CheckCodeGenerator{
        /**
         * 验证码生成
         * @return 验证码
         */
        String generate(int length);
        

    }

    /**
     * @description key生成器
     * @author Mr.M
     * @date 2022/9/29 16:34
     */
    public interface KeyGenerator{

        /**
         * key生成
         * @return 验证码
         */
        String generate(String prefix);
    }


    /**
     * @description 验证码存储
     * @author Mr.M
     * @date 2022/9/29 16:34
     */
    public interface CheckCodeStore{

        /**
         * @description 向缓存设置key
         * @param key key
         * @param value value
         * @param expire 过期时间,单位秒
         * @return void
         * @author Mr.M
         * @date 2022/9/29 17:15
        */
        void set(String key, String value, Integer expire);

        String get(String key);

        void remove(String key);
    }
}
