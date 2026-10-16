package com.smart.university.service;

import com.smart.university.domain.dto.req.TeacherWorkloadQueryReqDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadDetailRespDTO;
import com.smart.university.domain.dto.resp.TeacherWorkloadPageRespDTO;
import com.smart.university.domain.entity.StatTeacherWorkloadDO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教师工作量统计服务（S-04）
 */
public interface TeacherWorkloadService {

    /**
     * 分页查询教师工作量，教务端可查全部
     *
     * @param requestParam 查询条件
     * @return 教师工作量分页结果与口径回显
     */
    TeacherWorkloadPageRespDTO pageTeacherWorkload(TeacherWorkloadQueryReqDTO requestParam);

    /**
     * 查询教师本人工作量，教师身份从登录态取
     *
     * @param semesterId 学期 ID，为空取当前学期
     * @return 教师本人工作量
     */
    TeacherWorkloadDetailRespDTO getMyWorkload(Long semesterId);

    /**
     * 查询指定教师的工作量
     *
     * @param teacherId  教师 ID
     * @param semesterId 学期 ID，为空取当前学期
     * @return 教师工作量详情
     */
    TeacherWorkloadDetailRespDTO getTeacherWorkload(Long teacherId, Long semesterId);

    /**
     * 查询本学期教师人均课时
     *
     * @param semesterId 学期 ID，为空取当前学期
     * @return 人均课时，保留 1 位小数
     */
    BigDecimal getAverageWorkload(Long semesterId);

    /**
     * 构建教师工作量快照数据
     *
     * @param semesterId 学期 ID
     * @return 教师工作量快照集合
     */
    List<StatTeacherWorkloadDO> listTeacherWorkloadSnapshot(Long semesterId);
}
