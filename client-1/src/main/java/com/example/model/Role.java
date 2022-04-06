package com.example.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

@Data
public class Role implements GrantedAuthority {

    private Integer id;

    // authority 需要加上前缀 ROLE_
    private String name;

    @Override
    public String getAuthority() {
        return name;
    }
}
