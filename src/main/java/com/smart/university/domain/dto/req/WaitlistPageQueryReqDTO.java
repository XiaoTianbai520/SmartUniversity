package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * 学生候补列表分页查询入参，学生身份由后端根据 Token 推导
 */
@Data
public class WaitlistPageQueryReqDTO extends PageQueryReqDTO {

    /**
     * 学期 ID，不传时使用当前学期
     */
    @Schema(example = "1")
    private Long semesterId;
}
