package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.CommonError;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableItemDto;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/2 16:12
 */
@Slf4j
@Service
public class MyCourseTablesServiceImpl implements MyCourseTablesService {

    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;

    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;

    @Autowired
    ContentServiceClient contentServiceClient;

    @Autowired
    MyCourseTablesServiceImpl myCourseTablesService;

    @Override
    public XcChooseCourseDto addChooseCourse(String userId,Long courseId) {
        //查询课程信息
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null){
            XueChengPlusException.cast("课程信息不存在");
        }
        Long id = coursepublish.getId();
        if(id==null){
            XueChengPlusException.cast(CommonError.UNKOWN_ERROR);
        }
        //课程收费标准
        String charge = coursepublish.getCharge();

        XcChooseCourse xcChooseCourse = null;
        if(charge.equals("201001")){
            //添加收费课程
            xcChooseCourse= myCourseTablesService.addChargeCoruse(userId,coursepublish);
        }else{
            //添加免费课程
            xcChooseCourse= myCourseTablesService.addFreeCoruse(userId,coursepublish);
        }


        XcChooseCourseDto xcChooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,xcChooseCourseDto);
        //获取学习资格
        XcCourseTablesDto xcCourseTablesDto = getLeanringStatus(userId, courseId);
        xcChooseCourseDto.setLearnStatus(xcCourseTablesDto.getLearnStatus());
        return xcChooseCourseDto;
    }


    /**
     * @description 判断学习资格
     * @param userId
     * @param courseId
     * @return XcCourseTablesDto 学习资格状态 [{"code":"702001","desc":"正常学习"},{"code":"702002","desc":"没有选课或选课后没有支付"},{"code":"702003","desc":"已过期需要申请续期或重新支付"}]
     * @author Mr.M
     * @date 2022/10/3 7:37
    */
    public XcCourseTablesDto getLeanringStatus(String userId, Long courseId){
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
            xcCourseTablesDto.setLearnStatus("702002");   //没有学习资格
            return xcCourseTablesDto;
        }
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
        //是否过期,true过期，false未过期
        boolean isExpires = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(!isExpires){
            //正常学习
            xcCourseTablesDto.setLearnStatus("702001");
           return xcCourseTablesDto;

        }else{
            //已过期
            xcCourseTablesDto.setLearnStatus("702003");
            return xcCourseTablesDto;
        }

    }

    @Override
    public boolean saveChooseCourseStauts(String choosecourseId) {
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(choosecourseId);
        if(xcChooseCourse!=null){
            String status = xcChooseCourse.getStatus();
            if("701002".equals(status)){//待支付
                //更新为选课成功
                xcChooseCourse.setStatus("701001");
                int update = xcChooseCourseMapper.updateById(xcChooseCourse);
                //添加到课程表
                addCourseTabls(xcChooseCourse);
                if(update>0){
                    log.debug("收到支付结果通知处理成功,选课记录:{}",xcChooseCourse);
                    return true;
                }else{
                    log.debug("收到支付结果通知处理失败,选课记录:{}",xcChooseCourse);
                    return false;
                }
            }else{
                log.debug("收到支付结果通知已经处理,选课记录:{}",xcChooseCourse);
                return true;
            }
        }else{
            log.debug("收到支付结果通知没有查询到关联的选课记录,choosecourseId:{}",choosecourseId);
        }
        return false;
    }


    /**
     * @description 根据课程和用户查询我的课程表中某一门课程
     * @param userId
     * @param courseId
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/2 17:07
    */
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId).eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }


    @Transactional
    //添加免费课程,免费课程加入选课记录表、我的课程表
    public XcChooseCourse addFreeCoruse(String userId, CoursePublish coursepublish) {
        //查询选课记录表是否存在免费的且选课成功的订单
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700001")//免费订单
                .eq(XcChooseCourse::getStatus, "701001");//选课成功
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }
        //添加选课记录信息
        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(0f);//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700001");//免费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701001");//选课成功

        xcChooseCourse.setValidDays(365);//免费课程默认365
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        xcChooseCourseMapper.insert(xcChooseCourse);
        //添加到我的课程表
        addCourseTabls(xcChooseCourse);
        return xcChooseCourse;

    }

    /**
     * @description 添加到我的课程表
     * @param xcChooseCourse 选课记录
     * @return com.xuecheng.learning.model.po.XcCourseTables
     * @author Mr.M
     * @date 2022/10/3 11:24
    */
    @Transactional
    public XcCourseTables addCourseTabls(XcChooseCourse xcChooseCourse){
        //选课记录完成且未过期可以添加课程到课程表
        String status = xcChooseCourse.getStatus();
        if (!"701001".equals(status)){
            XueChengPlusException.cast("选课记录未完成，无法添加到课程表");
        }
        //查询我的课程表
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            LocalDateTime validtimeEnd = xcChooseCourse.getValidtimeEnd();
            if(xcCourseTables.getValidtimeEnd().isAfter(validtimeEnd)){
                //如果我的课程表中的过期时间比新订单的过期时间靠后，不用更新课程表。
                return xcCourseTables;
            }else{
                //更新我的课程表
                xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
                xcCourseTables.setUpdateDate(LocalDateTime.now());
                xcCourseTables.setValidtimeStart(xcChooseCourse.getValidtimeStart());
                xcCourseTables.setValidtimeEnd(xcChooseCourse.getValidtimeStart());
                xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
                xcCourseTablesMapper.updateById(xcCourseTables);
                return xcCourseTables;
            }
        }
        XcCourseTables xcCourseTablesNew = new XcCourseTables();
        xcCourseTablesNew.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTablesNew.setUserId(xcChooseCourse.getUserId());
        xcCourseTablesNew.setCourseId(xcChooseCourse.getCourseId());
        xcCourseTablesNew.setCompanyId(xcChooseCourse.getCompanyId());
        xcCourseTablesNew.setCourseName(xcChooseCourse.getCourseName());
        xcCourseTablesNew.setCreateDate(LocalDateTime.now());
        xcCourseTablesNew.setValidtimeStart(xcChooseCourse.getValidtimeStart());
        xcCourseTablesNew.setValidtimeEnd(xcChooseCourse.getValidtimeEnd());
        xcCourseTablesNew.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTablesMapper.insert(xcCourseTablesNew);

        return xcCourseTablesNew;

    }

    @Transactional
    //添加收费课程
    public XcChooseCourse addChargeCoruse(String userId,CoursePublish coursepublish){

        //如果存在待支付记录直接返回
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(XcChooseCourse::getUserId, userId)
                .eq(XcChooseCourse::getCourseId, coursepublish.getId())
                .eq(XcChooseCourse::getOrderType, "700002")//收费订单
                .eq(XcChooseCourse::getStatus, "701002");//待支付
        List<XcChooseCourse> xcChooseCourses = xcChooseCourseMapper.selectList(queryWrapper);
        if (xcChooseCourses != null && xcChooseCourses.size()>0) {
            return xcChooseCourses.get(0);
        }

        XcChooseCourse xcChooseCourse = new XcChooseCourse();
        xcChooseCourse.setCourseId(coursepublish.getId());
        xcChooseCourse.setCourseName(coursepublish.getName());
        xcChooseCourse.setCoursePrice(coursepublish.getPrice());//免费课程价格为0
        xcChooseCourse.setUserId(userId);
        xcChooseCourse.setCompanyId(coursepublish.getCompanyId());
        xcChooseCourse.setOrderType("700002");//收费课程
        xcChooseCourse.setCreateDate(LocalDateTime.now());
        xcChooseCourse.setStatus("701002");//待支付

        xcChooseCourse.setValidDays(coursepublish.getValidDays());
        xcChooseCourse.setValidtimeStart(LocalDateTime.now());
        xcChooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(coursepublish.getValidDays()));
        xcChooseCourseMapper.insert(xcChooseCourse);
        return xcChooseCourse;
    }

    public PageResult<MyCourseTableItemDto> mycourestabls( MyCourseTableParams params){

        int page = params.getPage();
        int size = params.getSize();
        int startIndex = (page-1)*size;
        params.setStartIndex(startIndex);

        List<MyCourseTableItemDto> myCourseTableItemDtos = xcCourseTablesMapper.myCourseTables(params);
        int total = xcCourseTablesMapper.myCourseTablesCount(params);


        PageResult pageResult = new PageResult();
        pageResult.setItems(myCourseTableItemDtos);
        pageResult.setCounts(total);
        pageResult.setPage(page);
        pageResult.setPageSize(size);
        return pageResult;

    }

}
