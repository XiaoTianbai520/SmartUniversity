package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.dto.req.MajorPageQueryReqDTO;
import com.smart.university.domain.dto.req.MajorSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.MajorRespDTO;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.service.MajorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 专业服务实现
 */
@Service
@RequiredArgsConstructor
public class MajorServiceImpl implements MajorService {

    private final MajorMapper majorMapper;

    @Override
    public PageResult<MajorRespDTO> pageMajor(MajorPageQueryReqDTO requestParam) {
        long total = majorMapper.countMajorByCondition(requestParam);
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<MajorDO> majorDOList = majorMapper.listMajorByCondition(requestParam);
        List<MajorRespDTO> records = majorDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), total);
    }

    @Override
    public MajorRespDTO getMajorDetail(Long majorId) {
        MajorDO majorDO = majorMapper.getMajorById(majorId);
        if (majorDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "专业不存在");
        }
        return convertToRespDTO(majorDO);
    }

    @Override
    public List<MajorRespDTO> listEnabledMajor() {
        MajorPageQueryReqDTO requestParam = new MajorPageQueryReqDTO();
        requestParam.setStatus(1);
        requestParam.setPage(1);
        requestParam.setPageSize(200);
        return majorMapper.listMajorByCondition(requestParam).stream().map(this::convertToRespDTO).toList();
    }

    @Override
    public Long saveMajor(MajorSaveReqDTO requestParam) {
        MajorDO majorDO = new MajorDO();
        majorDO.setMajorCode(requestParam.getMajorCode());
        majorDO.setMajorName(requestParam.getMajorName());
        if (requestParam.getId() == null) {
            majorDO.setStatus(1);
            majorMapper.saveMajor(majorDO);
            return majorDO.getId();
        }
        MajorDO existMajorDO = majorMapper.getMajorById(requestParam.getId());
        if (existMajorDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "专业不存在");
        }
        majorDO.setId(requestParam.getId());
        majorMapper.updateMajor(majorDO);
        return requestParam.getId();
    }

    @Override
    public void updateMajorStatus(Long majorId, StatusUpdateReqDTO requestParam) {
        MajorDO majorDO = new MajorDO();
        majorDO.setId(majorId);
        majorDO.setStatus(requestParam.getStatus());
        majorMapper.updateMajor(majorDO);
    }

    private MajorRespDTO convertToRespDTO(MajorDO majorDO) {
        MajorRespDTO result = BeanUtil.copyProperties(majorDO, MajorRespDTO.class);
        result.setMajorId(majorDO.getId());
        return result;
    }
}
