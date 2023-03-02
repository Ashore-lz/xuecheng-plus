package com.xuecheng.ucenter.service.Impl.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.service.Impl.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/20 10:54
 */
@Slf4j
@Service
public class UserServiceImpl implements UserDetailsService {

    @Autowired
    XcUserMapper userMapper;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    XcMenuMapper menuMapper;

    //传入的是AuthParamsDto的json串
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = null;
        try {
            //将认证参数转为AuthParamsDto类型
            authParamsDto = JSON.parseObject(s, AuthParamsDto.class);
        } catch (Exception e) {
            log.info("认证请求不符合项目要求:{}",s);
            throw new RuntimeException("认证请求数据格式不对");
        }
        //认证方式,
        String authType = authParamsDto.getAuthType();
        //从spring容器中拿具体的认证bean实例
        AuthService authService = applicationContext.getBean(authType + "_authservice", AuthService.class);
        //开始认证,认证成功拿到用户信息
        XcUserExt xcUserExt = authService.execute(authParamsDto);

        return getUserPrincipal(xcUserExt);
    }
    //根据XcUserExt对象构造一个UserDetails对象
    /**
     * @description 查询用户信息
     * @param user  用户id，主键
     * @return com.xuecheng.ucenter.model.po.XcUser 用户信息
     * @author Mr.M
     * @date 2022/9/29 12:19
     */
    public UserDetails getUserPrincipal(XcUserExt user){

        //权限列表，存放的用户权限
        List<String> permissionList = new ArrayList<>();

        //根据用户id查询数据库中他的权限
        List<XcMenu> xcMenus = menuMapper.selectPermissionByUserId(user.getId());
        xcMenus.forEach(menu->{
            permissionList.add(menu.getCode());
        });
        if(permissionList.size()==0){
            //用户权限,如果不加报Cannot pass a null GrantedAuthority collection
            permissionList.add("test");
        }

        String[] authorities= permissionList.toArray(new String[0]);
        //原来存的是账号，现在扩展为用户的全部信息(密码不要放)
        user.setPassword(null);
        String jsonString = JSON.toJSONString(user);
        UserDetails userDetails = User.withUsername(jsonString).password("").authorities(authorities).build();

        return userDetails;
    }

}
