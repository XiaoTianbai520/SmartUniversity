package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.req.SemesterSaveReqDTO;
import com.smart.university.domain.dto.req.SemesterStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SemesterRespDTO;

/**
 * 学期服务
 */
public interface SemesterService {

    /**
     * 分页查询学期
     *
     * @param requestParam 查询条件
     * @return 学期分页结果
     */
    PageResult<SemesterRespDTO> pageSemester(SemesterPageQueryReqDTO requestParam);

    /**
     * 查询学期详情
     *
     * @param semesterId 学期 ID
     * @return 学期信息
     */
    SemesterRespDTO getSemesterDetail(Long semesterId);

    /**
     * 保存学期
     *
     * @param requestParam 学期入参
     * @return 学期 ID
     */
    Long saveSemester(SemesterSaveReqDTO requestParam);

    /**
     * 修改学期状态
     *
     * @param semesterId   学期 ID
     * @param requestParam 状态入参
     */
    void updateSemesterStatus(Long semesterId, SemesterStatusUpdateReqDTO requestParam);

    /**
     * 获取当前生效学期，学生端缺省学期时使用
     *
     * @return 学期数据对象
     */
    SemesterDO getCurrentSemester();

    /**
     * 根据 ID 查询学期
     *
     * @param semesterId 学期 ID
     * @return 学期数据对象
     */
    SemesterDO getSemesterById(Long semesterId);
}
