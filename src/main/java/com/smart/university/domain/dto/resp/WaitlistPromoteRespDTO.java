package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 候补递补结果出参
 */
@Data
public class WaitlistPromoteRespDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 本次递补成功人数
     */
    private Integer promotedCount;

    /**
     * 本次因校验不通过而失效的候补人数
     */
    private Integer invalidCount;

    /**
     * 本次递补成功的学号集合
     */
    private List<String> promotedStudentNos = new ArrayList<>();
}
