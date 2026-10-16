package com.smart.university.service;

import com.smart.university.domain.dto.req.ClassroomUsageQueryReqDTO;
import com.smart.university.domain.dto.resp.ClassroomUsageExtraRespDTO;
import com.smart.university.domain.dto.resp.ClassroomUsagePageRespDTO;
import com.smart.university.domain.entity.StatClassroomUsageDO;

import java.util.List;

/**
 * 教室利用率统计服务（S-03）
 */
public interface ClassroomUsageService {

    /**
     * 按维度统计教室利用率
     *
     * @param requestParam 查询条件
     * @return 教室利用率分页结果与口径回显
     */
    ClassroomUsagePageRespDTO pageClassroomUsage(ClassroomUsageQueryReqDTO requestParam);

    /**
     * 查询教室利用率的口径回显与整体利用率
     *
     * @param semesterId 学期 ID，为空取当前学期
     * @return 口径回显
     */
    ClassroomUsageExtraRespDTO getUsageExtra(Long semesterId);

    /**
     * 构建教室利用率快照数据，按教室维度输出
     *
     * @param semesterId 学期 ID
     * @return 教室利用率快照集合
     */
    List<StatClassroomUsageDO> listClassroomUsageSnapshot(Long semesterId);
}
