package com.nebula.studio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nebula.studio.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE EMAIL = #{email} LIMIT 1")
    User selectByEmail(String email);
}
