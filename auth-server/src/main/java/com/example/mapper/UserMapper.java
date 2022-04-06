package com.example.mapper;

import com.example.model.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User getByUsername(@Param("username") String username);

}
