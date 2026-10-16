package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 成绩分布异常分页出参（40.3），在 PageResult 之外额外回显 extra
 */
@Data
public class ScoreAnomalyPageRespDTO {

    /**
     * 当前页数据
     */
    private List<ScoreAnomalyRespDTO> records;

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
    private ScoreAnomalyExtraRespDTO extra;
}
