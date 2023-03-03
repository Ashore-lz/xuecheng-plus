package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/26 11:24
 */
@Slf4j
@Component
public class ReceivePayNotifyService {

    @Autowired
    XcChooseCourseMapper chooseCourseMapper;

    @Autowired
    MyCourseTablesService myCourseTablesService;

    @Autowired

    RabbitTemplate rabbitTemplate;

    //监听支付结果通知队列
    @RabbitListener(queues = {PayNotifyConfig.PAYNOTIFY_QUEUE})
    public void receive(String message) {

        //解析消息
        MqMessage mqMessage = JSON.parseObject(message, MqMessage.class);
        //判断该消息是否自己的消息
        String messageType = mqMessage.getMessageType();
        //记录了订单类型
        String businessKey2 = mqMessage.getBusinessKey2();
        //只处理支付结果通知的消息,并且是学生购买课程的订单的消息
        if(PayNotifyConfig.MESSAGE_TYPE.equals(messageType) && "60201".equals(businessKey2)){
                //根据选课id查询选课表的记录
            String businessKey1 = mqMessage.getBusinessKey1();
            XcChooseCourse xcChooseCourse = chooseCourseMapper.selectById(businessKey1);
            if(xcChooseCourse == null){
                log.info("收到支付结果通知,查询不到选课记录,businessKey1:{}",businessKey1);
                return ;
            }

            XcChooseCourse xcChooseCourse_u = new XcChooseCourse();
            xcChooseCourse_u.setStatus("701001");//选课成功
            chooseCourseMapper.update(xcChooseCourse_u,new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getId,businessKey1));

            //查询最新的选课记录
            xcChooseCourse = chooseCourseMapper.selectById(businessKey1);
            //向我的课程表添加记录
            myCourseTablesService.addCourseTabls(xcChooseCourse);

            //发送回复
            send(mqMessage);

        }

    }

    /**
     * @description 回复消息
     * @param message  回复消息
     * @return void
     * @author Mr.M
     * @date 2022/9/20 9:43
     */
    public void send(MqMessage message){
        //转json
        String msg = JSON.toJSONString(message);
        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_REPLY_QUEUE, msg);
        log.debug("学习中心服务向订单服务回复消息:{}",message);
    }

}
