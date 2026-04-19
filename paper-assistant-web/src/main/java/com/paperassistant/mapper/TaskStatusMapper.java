package com.paperassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paperassistant.entity.TaskStatusEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskStatusMapper extends BaseMapper<TaskStatusEntity> {
}
