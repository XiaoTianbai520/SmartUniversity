package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 学生选课入参
 */
@Data
public class CourseSelectReqDTO {

    /**
     * 教学班 ID
     */
    @Schema(example = "1")
    @NotNull(message = "教学班 ID 不能为空")
    private Long teachingClassId;
}
