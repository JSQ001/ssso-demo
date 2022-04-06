package com.example.mapper;

import com.example.model.Role;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper {

    List<Role> getByUserId(@Param("userId") Integer userId);

}
