package com.xuecheng.base.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PageParams {

    public static final long DEFAULT_PAGE_CURRENT = 1L;
    //每页记录数默认值
    public static final long DEFAULT_PAGE_SIZE = 10L;

    @ApiModelProperty("当前页码")
    //当前页码
    private Long pageNo = DEFAULT_PAGE_CURRENT;

    @ApiModelProperty("每页记录数默认值")
    //每页记录数默认值
    private Long pageSize = DEFAULT_PAGE_SIZE;

    public PageParams(){
    }

    public PageParams(long pageNo,long pageSize){
        this.pageNo = pageNo;
        this.pageSize = pageSize;
    }

}
