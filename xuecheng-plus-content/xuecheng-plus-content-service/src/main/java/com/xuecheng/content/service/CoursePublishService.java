package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDto;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/17 10:56
 * @version 1.0
 */
public interface CoursePublishService {

  /**
   * @description 获取课程预览信息
   * @param courseId 课程id
   * @return com.xuecheng.content.model.dto.CoursePreviewDto
   * @author Mr.M
   * @date 2022/9/16 15:36
   */
  public CoursePreviewDto getCoursePreviewInfo(Long courseId);
  /**
   * @description 提交审核
   * @param courseId  课程id
   * @return void
   * @author Mr.M
   * @date 2022/9/18 10:31
   */
  public void commitAudit(Long companyId,Long courseId);
  /**
   * @description 课程发布接口
   * @param companyId 机构id
   * @param courseId 课程id
   * @return void
   * @author Mr.M
   * @date 2022/9/20 16:23
   */
  public void publish(Long companyId,Long courseId);

}
