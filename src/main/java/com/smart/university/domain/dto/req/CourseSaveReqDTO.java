package com.smart.university.domain.dto.req;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 课程新增 / 修改入参
 */
@Data
public class CourseSaveReqDTO {

    /**
     * 课程 ID，修改时必传
     */
    private Long id;

    /**
     * 课程编号
     */
    @NotBlank(message = "课程编号不能为空")
    private String courseCode;

    /**
     * 课程名称
     */
    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 课程学分
     */
    @NotNull(message = "课程学分不能为空")
    private BigDecimal credit;

    /**
     * REQUIRED / MAJOR_ELECTIVE / PUBLIC_ELECTIVE
     */
    @NotBlank(message = "课程性质不能为空")
    private String courseType;

    /**
     * 默认教学班容量
     */
    private Integer defaultCapacity;
}
