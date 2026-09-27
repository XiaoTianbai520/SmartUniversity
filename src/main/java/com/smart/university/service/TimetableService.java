package com.smart.university.service;

import com.smart.university.domain.dto.resp.TimetableRespDTO;

import java.util.List;

/**
 * 课表服务，V1 不建立独立课表表，由选课记录与排课动态生成
 */
public interface TimetableService {

    /**
     * 查询我的课表
     *
     * @param semesterId 学期 ID，为空时使用当前学期
     * @return 课表集合
     */
    List<TimetableRespDTO> listTimetable(Long semesterId);
}
