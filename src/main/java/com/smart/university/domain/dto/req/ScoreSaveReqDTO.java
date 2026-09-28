package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

/**
 * 教师保存成绩入参
 */
@Data
public class ScoreSaveReqDTO {

    /**
     * 百分制成绩
     */
    @Schema(example = "85")
    @NotNull(message = "成绩不能为空")
    private BigDecimal score;
}
