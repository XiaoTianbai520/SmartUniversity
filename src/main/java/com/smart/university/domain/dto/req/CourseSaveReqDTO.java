package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(example = "1")
    private Long id;

    /**
     * 课程编号
     */
    @Schema(example = "MATH101")
    @NotBlank(message = "课程编号不能为空")
    private String courseCode;

    /**
     * 课程名称
     */
    @Schema(example = "高等数学")
    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    /**
     * 课程简介
     */
    @Schema(example = "这是一段测试描述")
    private String description;

    /**
     * 课程学分
     */
    @Schema(example = "3")
    @NotNull(message = "课程学分不能为空")
    private BigDecimal credit;

    /**
     * REQUIRED / MAJOR_ELECTIVE / PUBLIC_ELECTIVE
     */
    @Schema(example = "REQUIRED")
    @NotBlank(message = "课程性质不能为空")
    private String courseType;

    /**
     * 默认教学班容量
     */
    @Schema(example = "50")
    private Integer defaultCapacity;
}
