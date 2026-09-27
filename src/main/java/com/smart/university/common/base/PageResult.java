package com.smart.university.common.base;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应结构
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 当前页码，从 1 开始
     */
    private long page;

    /**
     * 每页条数
     */
    private long pageSize;

    /**
     * 总记录数
     */
    private long total;

    public PageResult() {
    }

    public PageResult(List<T> records, long page, long pageSize, long total) {
        this.records = records;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    /**
     * 构造空分页结果
     *
     * @param page     当前页码
     * @param pageSize 每页条数
     * @return 空分页结果
     */
    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(Collections.emptyList(), page, pageSize, 0L);
    }

    /**
     * 转换分页结果中的记录类型
     *
     * @param converter 记录转换器
     * @return 转换后的分页结果
     */
    public <R> PageResult<R> convert(Function<? super T, ? extends R> converter) {
        PageResult<R> result = new PageResult<>();
        result.setPage(this.page);
        result.setPageSize(this.pageSize);
        result.setTotal(this.total);
        if (this.records == null) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        List<R> convertedRecords = new ArrayList<>(this.records.size());
        for (T each : this.records) {
            convertedRecords.add(converter.apply(each));
        }
        result.setRecords(convertedRecords);
        return result;
    }
}
