package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;

/**
 * 批次开放教学班入参
 */
@Data
public class BatchTeachingClassSaveReqDTO {

    /**
     * 教学班 ID 集合
     */
    @Schema(example = "[1, 2]")
    @NotEmpty(message = "教学班 ID 集合不能为空")
    private List<Long> teachingClassIds;
}
