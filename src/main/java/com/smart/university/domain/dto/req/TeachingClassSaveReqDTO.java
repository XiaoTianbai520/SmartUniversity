package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * 教学班新增 / 修改入参
 */
@Data
public class TeachingClassSaveReqDTO {

    /**
     * 教学班 ID，修改时必传
     */
    @Schema(example = "1")
    private Long id;

    /**
     * 教学班编号
     */
    @Schema(example = "CS2601")
    @NotBlank(message = "教学班编号不能为空")
    private String classCode;

    /**
     * 教学班名称
     */
    @Schema(example = "计算机2601班")
    @NotBlank(message = "教学班名称不能为空")
    private String className;

    /**
     * 课程 ID
     */
    @Schema(example = "1")
    @NotNull(message = "课程 ID 不能为空")
    private Long courseId;

    /**
     * 学期 ID
     */
    @Schema(example = "1")
    @NotNull(message = "学期 ID 不能为空")
    private Long semesterId;

    /**
     * 任课教师 ID
     */
    @Schema(example = "T10001")
    @NotNull(message = "教师 ID 不能为空")
    private Long teacherId;

    /**
     * 最大选课人数
     */
    @Schema(example = "50")
    @NotNull(message = "容量不能为空")
    private Integer capacity;

    /**
     * 开放专业 ID 集合
     */
    @Schema(example = "[1, 2]")
    private List<Long> majorIds;

    /**
     * 开放年级 ID 集合
     */
    @Schema(example = "[1, 2]")
    private List<Long> gradeIds;
}
