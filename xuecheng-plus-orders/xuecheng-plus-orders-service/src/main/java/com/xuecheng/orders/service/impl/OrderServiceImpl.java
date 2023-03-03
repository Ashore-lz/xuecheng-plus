package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description 订单接口实现类
 * @date 2022/10/25 11:42
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    XcOrdersMapper ordersMapper;

    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;

    @Autowired
    XcPayRecordMapper payRecordMapper;

    @Autowired
    MqMessageService mqMessageService;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {

        //创建商品订单
        XcOrders orders = saveXcOrders(userId, addOrderDto);
        //添加支付记录
        XcPayRecord payRecord = createPayRecord(orders);

        //生成支付二维码
        String qrCode = null;
        try {
            //url要可以被模拟器访问到，url为下单接口(稍后定义)
            qrCode = new QRCodeUtil().createQRCode("http://192.168.101.1/api/orders/requestpay?payNo="+payRecord.getPayNo(), 200, 200);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        //封装要返回的数据
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        //支付二维码
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        LambdaQueryWrapper<XcPayRecord> queryWrapper = new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo);
        return payRecordMapper.selectOne(queryWrapper);
    }

    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        //当支付成功才更新订单状态
        //首先判断是否支付成功
        String trade_status = payStatusDto.getTrade_status();
        if(trade_status.equals("TRADE_SUCCESS")){

            //支付成功了才去更新订单状态
            //拿到支付记录交易号
            String payNo = payStatusDto.getOut_trade_no();
            XcPayRecord payRecord = getPayRecordByPayno(payNo);

            if(payRecord == null){
                log.info("收到支付结果通知查询不到支付记录,收到的信息：{}",payStatusDto);
                return ;
            }

            //支付结果
            String status = payRecord.getStatus();
            if("601002".equals(status)){
                log.info("收到支付结果通知，支付记录状态已经为支付成功，不进行任务操作");
                return ;
            }
            //支付宝传给我们appid
            String appid_alipay = payStatusDto.getApp_id();
            //支付记录表中记录的总金额
            int totalPriceDb = (int) (payRecord.getTotalPrice()*100);//转成分
            int total_amount = (int) (Float.parseFloat(payStatusDto.getTotal_amount())*100);//转成分
            if(totalPriceDb!=total_amount || !appid_alipay.equals(APP_ID)){
                log.info("收到支付结果通知，校验失败,支付宝参数appid:{},total_amount:{},我们自己的数据：appid：{},TotalPrice:{}",appid_alipay,payStatusDto.getTotal_amount(),APP_ID,totalPriceDb);
                return ;
            }
            //首先要更新支付记录
            XcPayRecord payRecord_u = new XcPayRecord();
            payRecord_u.setStatus("601002");//支付成功
            payRecord_u.setOutPayNo(payStatusDto.getTrade_no());//支付宝自己的订单号
            payRecord_u.setOutPayChannel("603002");//通过支付宝支付
            payRecord_u.setPaySuccessTime(LocalDateTime.now());
            LambdaQueryWrapper<XcPayRecord> queryWrapper = new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo);
            int update = payRecordMapper.update(payRecord_u, queryWrapper);
            if(update>0){
                log.info("收到支付宝支付结果通知，更新支付记录表成功:{}",payStatusDto);
            }else{
                log.info("收到支付宝支付结果通知，更新支付记录表失败:{}",payStatusDto);
            }
            //获取订单
            Long orderId = payRecord.getOrderId();//订单id
            XcOrders orders = ordersMapper.selectById(orderId);
            if(orders == null){
                log.info("收到支付宝支付结果通知，查询不到订单,支付宝传过来的参数:{},订单号:{}",payStatusDto,orderId);
                return ;
            }

            //再更新订单状态
            XcOrders orders_u = new XcOrders();
            orders_u.setStatus("600002");//更新订单状态为支付成功
            int update1 = ordersMapper.update(orders_u, new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getId, orderId));
            if(update1>0){
                log.info("收到支付宝支付结果通知，更新订单表成功,支付宝传过来的参数:{},订单号:{}",payStatusDto,orderId);
                //找到订单表所关联的外部业务系统的主键
                String outBusinessId = orders.getOutBusinessId();
                //向消息表插入记录，String messageType(标记为支付结果通知),String businessKey1,String businessKey2(具体订单业务类型),String businessKey3
                mqMessageService.addMessage(PayNotifyConfig.MESSAGE_TYPE,outBusinessId,orders.getOrderType(),null);

            }else{
                log.info("收到支付宝支付结果通知，更新订单表失败,支付宝传过来的参数:{},订单号:{}",payStatusDto,orderId);
            }

        }

    }
//    public void saveWxPayStatus(PayStatusDto payStatusDto) {
//        //支付渠道编号603001
//        //先根据支付记录交易号查询支付记录
//        //从支付记录中拿到订单号，查询订单
//        //更新订单的状态
//    }

    //添加支付记录
    public XcPayRecord createPayRecord(XcOrders orders){
        XcPayRecord payRecord = new XcPayRecord();
        long payNo = IdWorkerUtils.getInstance().nextId();
        payRecord.setPayNo(payNo);//支付记录交易号
        //记录关键订单id
        payRecord.setOrderId(orders.getId());
        payRecord.setOrderName(orders.getOrderName());
        payRecord.setTotalPrice(orders.getTotalPrice());
        payRecord.setCurrency("CNY");
        payRecord.setCreateDate(LocalDateTime.now());
        payRecord.setStatus("601001");//未支付
        payRecord.setUserId(orders.getUserId());
        payRecordMapper.insert(payRecord);
        return payRecord;
    }

    //创建商品订单
    @Transactional
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {

        //选课记录id
        String outBusinessId = addOrderDto.getOutBusinessId();
        //对订单插入进行幂等性处理
        //根据选课记录id从数据库查询订单信息
        XcOrders orders = getOrderByBusinessId(outBusinessId);
        if(orders!=null){
            return orders;
        }

        //添加订单
        orders = new XcOrders();
        long orderId = IdWorkerUtils.getInstance().nextId();//订单号
        orders.setId(orderId);
        orders.setTotalPrice(addOrderDto.getTotalPrice());
        orders.setCreateDate(LocalDateTime.now());
        orders.setStatus("600001");//未支付
        orders.setUserId(userId);
        orders.setOrderType(addOrderDto.getOrderType());
        orders.setOrderName(addOrderDto.getOrderName());
        orders.setOrderDetail(addOrderDto.getOrderDetail());
        orders.setOrderDescrip(addOrderDto.getOrderDescrip());
        orders.setOutBusinessId(addOrderDto.getOutBusinessId());//选课记录id
        ordersMapper.insert(orders);
        //插入订单明细表
        String orderDetailJson = addOrderDto.getOrderDetail();
        //将json转成List
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        //将明细List插入数据库
        xcOrdersGoods.forEach(ordersGodds->{
            //在明细中记录订单号
            ordersGodds.setOrderId(orderId);
            ordersGoodsMapper.insert(ordersGodds);
        });

        return orders;


    }

    //根据业务id查询订单
    public XcOrders getOrderByBusinessId(String businessId) {
        XcOrders orders = ordersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return orders;
    }

}
