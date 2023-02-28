package com.xuecheng.content.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/10 15:04
 * @version 1.0
 */
@Data
public class SaveTeachplanDto {


 /***
  * 教学计划id
  */
 private Long id;

 /**
  * 课程计划名称
  */
 private String pname;

 /**
  * 课程计划父级Id
  */
 private Long parentid;

 /**
  * 层级，分为1、2、3级
  */
 private Integer grade;

 /**
  * 课程类型:1视频、2文档
  */
 private String mediaType;

 /**
  * 开始直播时间
  */
 private LocalDateTime startTime;

 /**
  * 直播结束时间
  */
 private LocalDateTime endTime;


 /**
  * 排序字段
  */
// private Integer orderby;

 /**
  * 课程标识
  */
 private Long courseId;

 /**
  * 课程发布标识
  */
 private Long coursePubId;


 /**
  * 是否支持试学或预览（试看）
  */
 private String isPreview;
}
