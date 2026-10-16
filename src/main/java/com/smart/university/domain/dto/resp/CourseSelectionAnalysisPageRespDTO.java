package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 课程选课分析分页出参（37.3），在 PageResult 之外额外回显 extra
 */
@Data
public class CourseSelectionAnalysisPageRespDTO {

    /**
     * 当前页数据
     */
    private List<CourseSelectionAnalysisRespDTO> records;

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
    private CourseSelectionAnalysisExtraRespDTO extra;
}
