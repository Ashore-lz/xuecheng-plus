package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/10 11:52
 * @version 1.0
 */
@Data
public class TeachplanDto extends Teachplan {

   //课程计划关联的媒资信息
   TeachplanMedia teachplanMedia;

    //子目录
   List<TeachplanDto> teachPlanTreeNodes;
}
