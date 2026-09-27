package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.SemesterDO;
import com.smart.university.domain.enums.SemesterStatusEnum;
import com.smart.university.domain.dto.req.SemesterPageQueryReqDTO;
import com.smart.university.domain.dto.req.SemesterSaveReqDTO;
import com.smart.university.domain.dto.req.SemesterStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SemesterRespDTO;
import com.smart.university.mapper.SemesterMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 学期服务实现
 */
@Service
@RequiredArgsConstructor
public class SemesterServiceImpl extends ServiceImpl<SemesterMapper, SemesterDO> implements SemesterService {

    private final SemesterMapper semesterMapper;

    @Override
    public PageResult<SemesterRespDTO> pageSemester(SemesterPageQueryReqDTO requestParam) {
        Page<SemesterDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<SemesterDO> pageResult = semesterMapper.listSemesterByCondition(page, requestParam);
        List<SemesterDO> semesterDOList = pageResult.getRecords();
        List<SemesterRespDTO> records = semesterDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public SemesterRespDTO getSemesterDetail(Long semesterId) {
        return convertToRespDTO(getSemesterById(semesterId));
    }

    @Override
    public Long saveSemester(SemesterSaveReqDTO requestParam) {
        if (!requestParam.getStartDate().isBefore(requestParam.getEndDate())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "学期开始日期必须早于结束日期");
        }
        SemesterDO semesterDO = new SemesterDO();
        semesterDO.setSemesterCode(requestParam.getSemesterCode());
        semesterDO.setAcademicYear(requestParam.getAcademicYear());
        semesterDO.setTermNo(requestParam.getTermNo());
        semesterDO.setStartDate(requestParam.getStartDate());
        semesterDO.setEndDate(requestParam.getEndDate());
        semesterDO.setMaxSelectionCredit(requestParam.getMaxSelectionCredit());
        if (requestParam.getId() == null) {
            semesterDO.setStatus(SemesterStatusEnum.PLANNED);
            semesterMapper.saveSemester(semesterDO);
            return semesterDO.getId();
        }
        semesterDO.setId(requestParam.getId());
        semesterMapper.updateSemester(semesterDO);
        return requestParam.getId();
    }

    @Override
    public void updateSemesterStatus(Long semesterId, SemesterStatusUpdateReqDTO requestParam) {
        SemesterStatusEnum semesterStatusEnum = SemesterStatusEnum.getByCode(requestParam.getStatus());
        if (semesterStatusEnum == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "学期状态不合法");
        }
        SemesterDO semesterDO = new SemesterDO();
        semesterDO.setId(semesterId);
        semesterDO.setStatus(semesterStatusEnum);
        semesterMapper.updateSemester(semesterDO);
    }

    @Override
    public SemesterDO getCurrentSemester() {
        SemesterDO result = semesterMapper.getCurrentSemester();
        if (result == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "当前没有生效中的学期");
        }
        return result;
    }

    @Override
    public SemesterDO getSemesterById(Long semesterId) {
        SemesterDO result = semesterMapper.getSemesterById(semesterId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "学期不存在");
        }
        return result;
    }

    private SemesterRespDTO convertToRespDTO(SemesterDO semesterDO) {
        SemesterRespDTO result = BeanUtil.copyProperties(semesterDO, SemesterRespDTO.class);
        result.setSemesterId(semesterDO.getId());
        result.setStatus(semesterDO.getStatus() == null ? null : semesterDO.getStatus().name());
        return result;
    }
}
