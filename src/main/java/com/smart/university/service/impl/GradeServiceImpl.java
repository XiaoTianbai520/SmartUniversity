package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.dto.req.GradePageQueryReqDTO;
import com.smart.university.domain.dto.req.GradeSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.GradeRespDTO;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.service.GradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 年级服务实现
 */
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeCohortMapper gradeCohortMapper;

    @Override
    public PageResult<GradeRespDTO> pageGrade(GradePageQueryReqDTO requestParam) {
        long total = gradeCohortMapper.countGradeCohortByCondition(requestParam);
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<GradeCohortDO> gradeDOList = gradeCohortMapper.listGradeCohortByCondition(requestParam);
        List<GradeRespDTO> records = gradeDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), total);
    }

    @Override
    public GradeRespDTO getGradeDetail(Long gradeId) {
        GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(gradeId);
        if (gradeCohortDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "年级不存在");
        }
        return convertToRespDTO(gradeCohortDO);
    }

    @Override
    public List<GradeRespDTO> listEnabledGrade() {
        GradePageQueryReqDTO requestParam = new GradePageQueryReqDTO();
        requestParam.setStatus(1);
        requestParam.setPage(1);
        requestParam.setPageSize(200);
        return gradeCohortMapper.listGradeCohortByCondition(requestParam).stream()
                .map(this::convertToRespDTO).toList();
    }

    @Override
    public Long saveGrade(GradeSaveReqDTO requestParam) {
        GradeCohortDO gradeCohortDO = new GradeCohortDO();
        gradeCohortDO.setGradeName(requestParam.getGradeName());
        gradeCohortDO.setEntryYear(requestParam.getEntryYear());
        if (requestParam.getId() == null) {
            gradeCohortDO.setStatus(1);
            gradeCohortMapper.saveGradeCohort(gradeCohortDO);
            return gradeCohortDO.getId();
        }
        GradeCohortDO existGradeDO = gradeCohortMapper.getGradeCohortById(requestParam.getId());
        if (existGradeDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "年级不存在");
        }
        gradeCohortDO.setId(requestParam.getId());
        gradeCohortMapper.updateGradeCohort(gradeCohortDO);
        return requestParam.getId();
    }

    @Override
    public void updateGradeStatus(Long gradeId, StatusUpdateReqDTO requestParam) {
        GradeCohortDO gradeCohortDO = new GradeCohortDO();
        gradeCohortDO.setId(gradeId);
        gradeCohortDO.setStatus(requestParam.getStatus());
        gradeCohortMapper.updateGradeCohort(gradeCohortDO);
    }

    private GradeRespDTO convertToRespDTO(GradeCohortDO gradeCohortDO) {
        GradeRespDTO result = BeanUtil.copyProperties(gradeCohortDO, GradeRespDTO.class);
        result.setGradeId(gradeCohortDO.getId());
        return result;
    }
}
