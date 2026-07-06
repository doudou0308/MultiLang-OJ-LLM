package com.exam.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.pojo.entity.CheckIn;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn> {
}
