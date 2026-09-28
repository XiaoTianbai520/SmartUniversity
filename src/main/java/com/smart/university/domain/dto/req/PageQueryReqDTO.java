package com.smart.university.domain.dto.req;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import jakarta.validation.constraints.Min;

/**
 * 分页查询基础入参
 */
@Data
public class PageQueryReqDTO {

    /**
     * 页码，从 1 开始，默认 1
     */
    @Schema(example = "1")
    @Min(value = 1, message = "页码最小为 1")
    private Integer page;

    /**
     * 每页条数，默认 20
     */
    @Schema(example = "10")
    @Min(value = 1, message = "每页条数最小为 1")
    private Integer pageSize;

    /**
     * 计算数据库查询偏移量
     *
     * @return 偏移量
     */
    public long getOffset() {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = getLimit();
        return (long) (currentPage - 1) * currentPageSize;
    }

    /**
     * 获取每页条数，未传或非法时取默认值 20
     *
     * @return 每页条数
     */
    public int getLimit() {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }

    /**
     * 获取当前页码，未传或非法时取默认值 1
     *
     * @return 当前页码
     */
    public int getCurrentPage() {
        return page == null || page < 1 ? 1 : page;
    }
}
