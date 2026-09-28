package com.smart.university.service;

import com.smart.university.common.base.PageResult;
import com.smart.university.domain.dto.req.WaitlistPageQueryReqDTO;
import com.smart.university.domain.dto.resp.WaitlistPromoteRespDTO;
import com.smart.university.domain.dto.resp.WaitlistRespDTO;

/**
 * 候补选课服务
 */
public interface WaitlistService {

    /**
     * 加入候补队列，教学班必须已满且在选课批次范围内
     *
     * @param teachingClassId 教学班 ID
     * @return 候补位次信息
     */
    WaitlistRespDTO joinWaitlist(Long teachingClassId);

    /**
     * 取消候补，取消后其他候补位次不重排
     *
     * @param selectionId 选课记录 ID
     */
    void cancelWaitlist(Long selectionId);

    /**
     * 分页查询当前学生的候补列表
     *
     * @param requestParam 查询条件
     * @return 候补列表分页结果
     */
    PageResult<WaitlistRespDTO> pageWaitlist(WaitlistPageQueryReqDTO requestParam);

    /**
     * 尝试按候补序号递补教学班剩余名额，退课、容量上调与教务手动触发共用
     *
     * @param teachingClassId 教学班 ID
     * @return 递补结果
     */
    WaitlistPromoteRespDTO tryPromote(Long teachingClassId);
}
