package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.CourseCategory;
import lombok.Data;

import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/8 11:29
 * @version 1.0
 */
 @Data
public class CourseCategoryTreeDto extends CourseCategory {
     //子分类
     List<CourseCategoryTreeDto> childrenTreeNodes;
}
