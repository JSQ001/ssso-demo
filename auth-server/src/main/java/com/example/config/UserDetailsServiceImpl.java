package com.example.config;

import com.example.model.User;
import com.example.service.RoleService;
import com.example.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *  实现 UserDetailsService ，该接口定义根据用户名获取用户所有信息，包括用户和权限
 */
@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userService.getByUsername(username);
        user.setAuthorities(roleService.getByUserId(user.getId()));

        user = new User();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("123456"));

        return user;
    }
}
