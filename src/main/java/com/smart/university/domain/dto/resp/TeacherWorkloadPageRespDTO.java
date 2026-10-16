package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 教师工作量分页出参（37.2），在 PageResult 之外额外回显 extra
 */
@Data
public class TeacherWorkloadPageRespDTO {

    /**
     * 当前页数据
     */
    private List<TeacherWorkloadRespDTO> records;

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
     * 口径回显
     */
    private TeacherWorkloadExtraRespDTO extra;
}
