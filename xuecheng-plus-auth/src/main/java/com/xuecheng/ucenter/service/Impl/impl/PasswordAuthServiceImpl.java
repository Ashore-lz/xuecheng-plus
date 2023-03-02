package com.xuecheng.ucenter.service.Impl.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.feignclient.CheckCodeClient;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.Impl.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author Mr.M
 * @version 1.0
 * @description 账号密码认证
 * @date 2022/10/20 14:49
 */
@Slf4j
@Service("password_authservice")
public class PasswordAuthServiceImpl implements AuthService {

    @Autowired
    XcUserMapper userMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    CheckCodeClient checkCodeClient;

    //实现账号和密码认证
    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

        //得到验证码
//        String checkcode = authParamsDto.getCheckcode();
//        String checkcodekey = authParamsDto.getCheckcodekey();
//        if(StringUtils.isBlank(checkcodekey) || StringUtils.isBlank(checkcode)){
//            throw new RuntimeException("验证码为空");
//
//        }
//
//        //校验验证码,请求验证码服务进行校验
//        Boolean result = checkCodeClient.verify(checkcodekey, checkcode);
//        if(result==null || !result){
//            throw new RuntimeException("验证码错误");
//        }

        //账号
        String username = authParamsDto.getUsername();
        //从数据库查询用户信息
        XcUser xcUser = userMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
        if (xcUser == null) {
            //账号不存在
            throw new RuntimeException("账号不存在");
        }
        //比对密码
        String passwordDB = xcUser.getPassword();//正确的密码(加密后)
        String passwordInput = authParamsDto.getPassword();//输入的密码
        boolean matches = passwordEncoder.matches(passwordInput, passwordDB);
        if(!matches){
            throw new RuntimeException("账号或密码错误");
        }
        XcUserExt xcUserExt = new XcUserExt();
        BeanUtils.copyProperties(xcUser,xcUserExt);
        return xcUserExt;
    }
}
