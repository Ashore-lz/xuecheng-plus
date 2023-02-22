package com.xuecheng.content.model.dto;

import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import lombok.Data;

import java.util.List;

@Data
public class TeachplanDto extends Teachplan {

    //关联媒资信息
    TeachplanMedia teachplanMedia;

    //子目录
    List<TeachplanDto> teachPlanTreeNodes;

}
