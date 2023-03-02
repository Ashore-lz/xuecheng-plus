package com.xuecheng.learning.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @description 消息队列配置
 * @author Mr.M
 * @date 2022/10/4 22:25
 * @version 1.0
 */
@Configuration
public class PayNotifyConfig {

  //交换机
  public static final String PAYNOTIFY_EXCHANGE_FANOUT = "paynotify_exchange_fanout";
  //队列名称
  public static final String CHOOSECOURSE_PAYNOTIFY_QUEUE = "choosecourse_paynotify_queue";
  public static final String PAYNOTIFY_REPLY_QUEUE = "paynotify_reply_queue";
  //声明交换机
  @Bean(PAYNOTIFY_EXCHANGE_FANOUT)
  public FanoutExchange paynotify_exchange_direct(){
   // 三个参数：交换机名称、是否持久化、当没有queue与其绑定时是否自动删除
   return new FanoutExchange(PAYNOTIFY_EXCHANGE_FANOUT, true, false);
  }

  //声明队列
  @Bean(CHOOSECOURSE_PAYNOTIFY_QUEUE)
  public Queue course_publish_queue(){
    return QueueBuilder.durable(CHOOSECOURSE_PAYNOTIFY_QUEUE).build();
  }
  //支付结果回复队列
  @Bean(PAYNOTIFY_REPLY_QUEUE)
  public Queue msgnotify_result_queue(){
    return QueueBuilder.durable(PAYNOTIFY_REPLY_QUEUE).build();
  }
  //交换机和队列绑定
  @Bean
  public Binding binding_course_publish_queue(@Qualifier(CHOOSECOURSE_PAYNOTIFY_QUEUE) Queue queue, @Qualifier(PAYNOTIFY_EXCHANGE_FANOUT) FanoutExchange exchange){
    return BindingBuilder.bind(queue).to(exchange);
  }

}
