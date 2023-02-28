package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author Mr.M
 * @version 1.0
 * @description 课程发布任务
 * @date 2022/10/17 17:11
 */
@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {

    @Autowired
    CoursePublishService coursePublishService;

    //课程发布任务执行入口，由xxl-job调度
    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception {
        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        log.debug("shardIndex="+shardIndex+",shardTotal="+shardTotal);
        //参数:分片序号、分片总数、消息类型、一次最多取到的任务数量、一次任务调度执行的超时时间
        process(shardIndex,shardTotal,"course_publish",5,60);
    }


    //课程发布执行逻辑
    @Override
    public boolean execute(MqMessage mqMessage) {

        log.debug("开始执行课程发布任务,课程id:{}",mqMessage.getBusinessKey1());

        //课程id
        Long courseId = Long.parseLong(mqMessage.getBusinessKey1());

        //课程静态化
        generateCourseHtml(mqMessage,courseId);


        //课程缓存
//        saveCourseCache(mqMessage,courseId);

        //创建课程索引
        saveCourseIndex(mqMessage,courseId);


        return true;
    }

    //课程静态化
    private void saveCourseIndex(MqMessage mqMessage,Long courseId){
        //任务id
        Long id = mqMessage.getId();
        //作消息幂等性处理
        //如果该阶段任务完成了不再处理直接返回
        int stageTwo = this.getMqMessageService().getStageTwo(id);//第二阶段的状态
        if(stageTwo>0){
            log.debug("当前阶段是创建课程索引,已经完成不再处理,任务信息:{}",mqMessage);
            return ;
        }

        //调用service创建索引
        coursePublishService.saveCourseIndex(courseId);


        //给该阶段任务打上完成标记
        this.getMqMessageService().completedStageTwo(id);//完成第二阶段的任务
    }
    //课程静态化
    private void generateCourseHtml(MqMessage mqMessage,Long courseId){
        //任务id
        Long id = mqMessage.getId();
        //作消息幂等性处理
        //如果该阶段任务完成了不再处理直接返回
        int stageOne = this.getMqMessageService().getStageOne(id);//第一阶段的状态
        if(stageOne>0){
            log.debug("当前阶段是静态化课程信息任务已经完成不再处理,任务信息:{}",mqMessage);
            return ;
        }

        //将课程信息进行静态化
        //调用service将课程静态化
        File file = coursePublishService.generateCourseHtml(courseId);
        if(file == null){
            XueChengPlusException.cast("课程静态化异常");
        }
        //将静态页面上传到minIO
        coursePublishService.uploadCourseHtml(courseId,file);
        //给该阶段任务打上完成标记
        this.getMqMessageService().completedStageOne(id);//完成第一阶段的任务
    }


}
