package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 教室利用率分页出参（37.1），在 PageResult 之外额外回显口径 extra
 */
@Data
public class ClassroomUsagePageRespDTO {

    /**
     * 当前页数据
     */
    private List<ClassroomUsageRespDTO> records;

    /**
     * 当前页码
     */
    private long page;

    /**
     * 每页条数
     */
    private long pageSize;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 统计口径回显
     */
    private ClassroomUsageExtraRespDTO extra;
}
