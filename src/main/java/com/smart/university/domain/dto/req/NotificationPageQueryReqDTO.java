package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知分页查询入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 通知类型
     */
    @Schema(example = "SELECTION_START")
    private String noticeType;

    /**
     * 是否已读：0 未读，1 已读
     */
    @Schema(example = "0")
    @Min(value = 0, message = "是否已读只能为 0 或 1")
    @Max(value = 1, message = "是否已读只能为 0 或 1")
    private Integer isRead;
}
