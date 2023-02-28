package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/8 11:25
 * @version 1.0
 */
@Api(value = "课程分类查询接口",tags = "课程分类查询接口")
 @RestController
public class CourseCategoryController {

    @Autowired
    CourseCategoryService CourseCategoryService;

     @GetMapping("/course-category/tree-nodes")
    public List<CourseCategoryTreeDto> queryTreeNodes(){


      return CourseCategoryService.queryTreeNodes();
    }

}
