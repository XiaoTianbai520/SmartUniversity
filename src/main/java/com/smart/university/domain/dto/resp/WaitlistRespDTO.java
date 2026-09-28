package com.smart.university.domain.dto.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 候补视图出参，加入候补与候补列表共用
 */
@Data
public class WaitlistRespDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 选课记录 ID，可作为取消候补的入参
     */
    private Long selectionId;

    /**
     * 教学班 ID
     */
    private Long teachingClassId;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 任课教师姓名
     */
    private String teacherName;

    /**
     * 课程学分
     */
    private BigDecimal credit;

    /**
     * 选课状态，取值 SELECTED / WAITING / WITHDRAWN
     */
    private String status;

    /**
     * 教学班容量
     */
    private Integer capacity;

    /**
     * 已选人数
     */
    private Integer selectedCount;

    /**
     * 该教学班候补总人数
     */
    private Integer waitingCount;

    /**
     * 候补序号，同一教学班内递增
     */
    private Integer waitlistNo;

    /**
     * 当前候补位次，实时计算得出
     */
    private Integer waitlistRank;

    /**
     * 加入候补时间
     */
    private LocalDateTime waitingAt;
}
