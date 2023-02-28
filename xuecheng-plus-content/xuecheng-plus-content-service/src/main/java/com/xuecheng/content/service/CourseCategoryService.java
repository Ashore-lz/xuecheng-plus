package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;

import java.util.List;

/**
 * @description 课程分类管理的接口
 * @author Mr.M
 * @date 2022/10/8 15:05
 * @version 1.0
 */
public interface CourseCategoryService {

 public List<CourseCategoryTreeDto> queryTreeNodes();
}
