package com.xuecheng.content.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;

/**
 * @description 课程管理service
 * @author Mr.M
 * @date 2022/10/8 9:44
 * @version 1.0
 */
public interface CourseBaseInfoService {


 /**
  * @description 课程查询
  * @param params 分页参数
  * @param queryCourseParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.content.model.po.CourseBase>
  * @author Mr.M
  * @date 2022/10/8 9:46
 */
  public PageResult<CourseBase>  queryCourseBaseList(PageParams params, QueryCourseParamsDto queryCourseParamsDto);


  /**
   * @description 新增课程
   * @param companyId  机构id
   * @param addCourseDto  添加课程的信息
   * @return com.xuecheng.content.model.dto.CourseBaseInfoDto
   * @author Mr.M
   * @date 2022/10/8 15:53
  */
  CourseBaseInfoDto createCourseBase(Long companyId,AddCourseDto addCourseDto);


  /**
   * @description 根据id查询课程信息
   * @param courseId 课程id
   * @return com.xuecheng.content.model.dto.CourseBaseInfoDto
   * @author Mr.M
   * @date 2022/10/10 10:58
  */
 public CourseBaseInfoDto getCourseBaseInfo(Long courseId);


 /**
  * @description 修改课程信息
  * @param companyId  机构id，要校验本机构只能修改本机构的课程
  * @param dto  课程信息
  * @return com.xuecheng.content.model.dto.CourseBaseInfoDto
  * @author Mr.M
  * @date 2022/9/8 21:04
  */
 public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto dto);

}
