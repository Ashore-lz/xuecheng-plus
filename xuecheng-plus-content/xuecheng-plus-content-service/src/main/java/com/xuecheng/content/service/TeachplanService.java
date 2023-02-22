package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    /**
     * @param courseId 课程id
     * @return List<TeachplanDto>
     * @description 查询课程计划树型结构
     */
    public List<TeachplanDto> findTeachplayTree(long courseId);

    public void saveTeachplan(SaveTeachplanDto teachplanDto);

}