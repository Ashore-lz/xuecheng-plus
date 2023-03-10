package com.xuecheng.content.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.feignclient.model.CourseIndex;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/10/17 10:57
 */
@Slf4j
@Service
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;

    @Autowired
    TeachplanService teachplanService;

    @Autowired
    CourseBaseMapper courseBaseMapper;

    @Autowired
    CourseMarketMapper courseMarketMapper;

    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    MqMessageService mqMessageService;

    @Autowired
    SearchServiceClient searchServiceClient;

    @Autowired
    MediaServiceClient mediaServiceClient;

    @Autowired
    CoursePublishMapper coursePublishMapper;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public CoursePreviewDto getCoursePreviewInfo(Long courseId) {

        //???????????????????????????
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);

        //????????????
        List<TeachplanDto> teachplayTree = teachplanService.findTeachplanTree(courseId);

        CoursePreviewDto coursePreviewDto = new CoursePreviewDto();
        coursePreviewDto.setCourseBase(courseBaseInfo);
        coursePreviewDto.setTeachplans(teachplayTree);
        return coursePreviewDto;
    }

    @Override
    public void commitAudit(Long companyId, Long courseId) {
        //????????????
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        //??????????????????
        String auditStatus = courseBase.getAuditStatus();
        //???????????????????????????????????????????????????
        if ("202003".equals(auditStatus)) {
            XueChengPlusException.cast("???????????????????????????????????????????????????????????????");
        }
        //??????????????????????????????????????????
        if (!courseBase.getCompanyId().equals(companyId)) {
            XueChengPlusException.cast("???????????????????????????????????????");
        }

        //????????????????????????
        if (StringUtils.isEmpty(courseBase.getPic())) {
            XueChengPlusException.cast("????????????????????????????????????");
        }

        //????????????????????????
        List<TeachplanDto> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if (teachplanTree.size() <= 0) {
            XueChengPlusException.cast("??????????????????????????????????????????");
        }

        //??????????????????????????????????????????????????????????????????????????????
        CoursePublishPre coursePublishPre = new CoursePublishPre();
        //??????????????????
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfo(courseId);
        BeanUtils.copyProperties(courseBaseInfo, coursePublishPre);
        //????????????????????????json
        String teachplanTreeJson = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachplanTreeJson);
        //??????????????????
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //??????json
        String courseMarketJson = JSON.toJSONString(courseMarket);
        //?????????????????????json??????????????????????????????
        coursePublishPre.setMarket(courseMarketJson);

        //????????????????????????????????????
        coursePublishPre.setStatus("202003");

        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if (coursePublishPre1 == null) {
            coursePublishPreMapper.insert(coursePublishPre);
        } else {
            coursePublishPreMapper.updateById(coursePublishPre);
        }

        //????????????????????????????????????
        courseBase.setAuditStatus("202003");
        courseBaseMapper.updateById(courseBase);

    }

    @Transactional
    @Override
    public void publish(Long companyId, Long courseId) {
        //????????????
        //????????????????????????
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre == null){
            XueChengPlusException.cast("??????????????????????????????????????????????????????");
        }
        //??????????????????????????????????????????
        if(!coursePublishPre.getCompanyId().equals(companyId)){
            XueChengPlusException.cast("???????????????????????????????????????");
        }


        //??????????????????
        String auditStatus = coursePublishPre.getStatus();
        //????????????????????????
        if(!"202004".equals(auditStatus)){
            XueChengPlusException.cast("????????????????????????????????????????????????");
        }

        //????????????????????????
        saveCoursePublish(courseId);

        //???????????????
        saveCoursePublishMessage(courseId);

        //????????????????????????????????????
        coursePublishPreMapper.deleteById(courseId);

    }

    @Override
    public File generateCourseHtml(Long courseId) {

        try {
            //??????freemarker
            Configuration configuration = new Configuration(Configuration.getVersion());

            //????????????
            //?????????????????????,classpath???templates???
            //??????classpath??????
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath + "/templates/"));
            //??????????????????
            configuration.setDefaultEncoding("utf-8");

            //????????????????????????
            Template template = configuration.getTemplate("course_template.ftl");

            //????????????
            CoursePreviewDto coursePreviewInfo = getCoursePreviewInfo(courseId);

            Map<String, Object> map = new HashMap<>();
            map.put("model", coursePreviewInfo);

            //?????????
            //??????1??????????????????2???????????????
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            System.out.println(content);
            //????????????????????????????????????
            InputStream inputStream = IOUtils.toInputStream(content);
            //?????????
            File course = File.createTempFile("course", ".html");
            FileOutputStream outputStream = new FileOutputStream(course);
            IOUtils.copy(inputStream, outputStream);
            return course;
        } catch (Exception e) {
            log.debug("?????????????????????:{}",e.getMessage(),e);
        }
        return null;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        //???file
        MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);
        //objectName ??????id.html
        String objectName = courseId+".html";
        //??????????????????????????????????????????
        String course = mediaServiceClient.upload(multipartFile, "course", objectName);
        if(course == null){
            XueChengPlusException.cast("??????????????????????????????????????????");
        }
    }

    @Override
    public Boolean saveCourseIndex(Long courseId) {

        //??????????????????????????????
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        //...????????????
        //????????????
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);

        //????????????????????????????????????
        Boolean result = searchServiceClient.add(courseIndex);
        if(!result){
            XueChengPlusException.cast("????????????????????????");
        }
        return result;
    }

    @Override
    public CoursePublish getCoursePublish(Long courseId) {
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        return coursePublish;
    }

    //????????????
//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//        //?????????????????????
//        String jsonString = (String) redisTemplate.opsForValue().get("course:" + courseId);
//        if(StringUtils.isNotEmpty(jsonString)){
//            //???json??????????????????
//            System.out.println("????????????============");
//            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
//            return coursePublish;
//        }else {
//            //??????????????????
//            System.out.println("???????????????===============");
//            CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
////            if(coursePublish!=null){
//                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish), 300+new Random().nextInt(100), TimeUnit.SECONDS);
////            }
//            return coursePublish;
//        }
//    }

    //?????????????????? ???????????????
//    @Override
//    public CoursePublish getCoursePublishCache(Long courseId) {
//        //?????????????????????
//        String jsonString = (String) redisTemplate.opsForValue().get("course:" + courseId);
//        if(StringUtils.isNotEmpty(jsonString)){
//            //???json??????????????????
//            System.out.println("????????????============");
//            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
//            return coursePublish;
//        }else {
//            synchronized (this){ //??????????????????  ???????????????????????????
//            jsonString = (String) redisTemplate.opsForValue().get("course:" + courseId);
//                if(StringUtils.isNotEmpty(jsonString)){  //????????????
//                    //???json??????????????????
//                    System.out.println("????????????============");
//                    CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
//                    return coursePublish;
//                }
//                //??????????????????
//                System.out.println("???????????????===============");
//                CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
////            if(coursePublish!=null){ //??????????????????
//                redisTemplate.opsForValue().set("course:" + courseId, JSON.toJSONString(coursePublish), 300+new Random().nextInt(100), TimeUnit.SECONDS);//??????????????????
////            }
//                return coursePublish;
//            }
//
//        }
//    }

    //?????????????????? ??????????????????
    @Override
    public CoursePublish getCoursePublishCache(Long courseId){
        //?????????????????????
        String jsonString = (String) redisTemplate.opsForValue().get("course:" + courseId);
        if(StringUtils.isNotEmpty(jsonString)){
//                System.out.println("========??????????????????===========");
            //???json??????????????????
            CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
            return coursePublish;
        }else{
            //??????setnx???redis????????????key?????????????????????????????????
//          Boolean lock001 = redisTemplate.opsForValue().setIfAbsent("lock001", "001",300,TimeUnit.SECONDS);
            //??????redisson?????????
            RLock lock = redissonClient.getLock("coursequery:" + courseId);
            //??????????????????
            lock.lock();
            try{
                //??????????????????????????????
                jsonString = (String) redisTemplate.opsForValue().get("course:" + courseId);
                if(StringUtils.isNotEmpty(jsonString)){
                    //???json??????????????????
                    CoursePublish coursePublish = JSON.parseObject(jsonString, CoursePublish.class);
                    return coursePublish;
                }
                System.out.println("??????????????????...");
                //?????????????????????????????????????????????
                CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
                //?????????????????????????????????????????????
                redisTemplate.opsForValue().set("course:" + courseId,JSON.toJSONString(coursePublish),300, TimeUnit.SECONDS);

                return coursePublish ;
            }finally {
                //?????????
                lock.unlock();
            }
        }
    }

    //????????????????????????
    private void saveCoursePublish(Long courseId) {

        //???????????????????????????????????????
        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);

        CoursePublish coursePublish = new CoursePublish();
        //???????????????????????????
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        coursePublish.setStatus("203002");//?????????

        CoursePublish coursePublishUpdate = coursePublishMapper.selectById(courseId);
        if(coursePublishUpdate == null){
            coursePublishMapper.insert(coursePublish);
        }else{
            coursePublishMapper.updateById(coursePublish);
        }
        //????????????????????????????????????
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("203002");//?????????
        courseBaseMapper.updateById(courseBase);
    }

    //???????????????
    private void saveCoursePublishMessage(Long courseId) {
        MqMessage mqMessage = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(mqMessage == null){
            XueChengPlusException.cast("????????????????????????");
        }
    }

}
