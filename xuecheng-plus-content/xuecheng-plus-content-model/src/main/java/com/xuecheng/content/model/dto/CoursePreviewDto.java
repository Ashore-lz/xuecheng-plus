package com.xuecheng.content.model.dto;

import lombok.Data;

import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/17 10:55
 */
@Data
public class CoursePreviewDto {

    //课程基本信息,课程营销信息
    CourseBaseInfoDto courseBase;


    //课程计划信息
    List<TeachplanDto> teachplans;

    //师资信息暂时不加...

}
