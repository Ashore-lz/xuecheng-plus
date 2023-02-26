package com.xuecheng.messagesdk;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @description 消息处理测试类，继承MessageProcessAbstract
 * @author Mr.M
 * @date 2022/9/21 21:44
 * @version 1.0
 */
@Slf4j
@Component
public class MessageProcessClass extends MessageProcessAbstract {


    @Autowired
    MqMessageService mqMessageService;

    //执行任务
    @Override
    public boolean execute(MqMessage mqMessage) {
        Long id = mqMessage.getId();
        log.debug("开始执行任务:{}",id);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        //取出阶段状态
        int stageOne = mqMessageService.getStageOne(id);
        if(stageOne<1){
            log.debug("开始执行第一阶段任务");
            System.out.println();
            int i = mqMessageService.completedStageOne(id);
            if(i>0){
                log.debug("完成第一阶段任务");
            }

        }else{
            log.debug("无需执行第一阶段任务");
        }

        return true;
    }
}
