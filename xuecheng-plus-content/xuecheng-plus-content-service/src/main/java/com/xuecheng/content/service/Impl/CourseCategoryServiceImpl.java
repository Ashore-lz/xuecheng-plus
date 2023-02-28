package com.xuecheng.content.service.Impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description TODO
 * @author Mr.M
 * @date 2022/10/8 15:05
 * @version 1.0
 */
@Slf4j
 @Service
public class CourseCategoryServiceImpl implements CourseCategoryService {

 @Autowired
 CourseCategoryMapper courseCategoryMapper;


 @Override
 public List<CourseCategoryTreeDto> queryTreeNodes() {
  return courseCategoryMapper.selectTreeNodes();
 }
}
