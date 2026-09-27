package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.AcademicClassDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.dto.req.AcademicClassPageQueryReqDTO;
import com.smart.university.domain.dto.req.AcademicClassSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.AcademicClassRespDTO;
import com.smart.university.mapper.AcademicClassMapper;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.service.AcademicClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 行政班服务实现
 */
@Service
@RequiredArgsConstructor
public class AcademicClassServiceImpl implements AcademicClassService {

    private final AcademicClassMapper academicClassMapper;

    private final MajorMapper majorMapper;

    private final GradeCohortMapper gradeCohortMapper;

    @Override
    public PageResult<AcademicClassRespDTO> pageAcademicClass(AcademicClassPageQueryReqDTO requestParam) {
        long total = academicClassMapper.countAcademicClassByCondition(requestParam);
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<AcademicClassDO> classDOList = academicClassMapper.listAcademicClassByCondition(requestParam);
        return new PageResult<>(convertToRespDTO(classDOList), requestParam.getCurrentPage(),
                requestParam.getLimit(), total);
    }

    @Override
    public AcademicClassRespDTO getAcademicClassDetail(Long classId) {
        AcademicClassDO academicClassDO = academicClassMapper.getAcademicClassById(classId);
        if (academicClassDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "行政班级不存在");
        }
        return convertToRespDTO(List.of(academicClassDO)).get(0);
    }

    @Override
    public Long saveAcademicClass(AcademicClassSaveReqDTO requestParam) {
        MajorDO majorDO = majorMapper.getMajorById(requestParam.getMajorId());
        if (majorDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "专业不存在");
        }
        GradeCohortDO gradeCohortDO = gradeCohortMapper.getGradeCohortById(requestParam.getGradeId());
        if (gradeCohortDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "年级不存在");
        }
        AcademicClassDO academicClassDO = new AcademicClassDO();
        academicClassDO.setClassCode(requestParam.getClassCode());
        academicClassDO.setClassName(requestParam.getClassName());
        academicClassDO.setMajorId(requestParam.getMajorId());
        academicClassDO.setGradeId(requestParam.getGradeId());
        if (requestParam.getId() == null) {
            academicClassDO.setStatus(1);
            academicClassMapper.saveAcademicClass(academicClassDO);
            return academicClassDO.getId();
        }
        academicClassDO.setId(requestParam.getId());
        academicClassMapper.updateAcademicClass(academicClassDO);
        return requestParam.getId();
    }

    @Override
    public void updateAcademicClassStatus(Long classId, StatusUpdateReqDTO requestParam) {
        AcademicClassDO academicClassDO = new AcademicClassDO();
        academicClassDO.setId(classId);
        academicClassDO.setStatus(requestParam.getStatus());
        academicClassMapper.updateAcademicClass(academicClassDO);
    }

    /**
     * 批量转换为出参，并补齐专业与年级名称
     *
     * @param classDOList 行政班数据对象集合
     * @return 行政班出参集合
     */
    private List<AcademicClassRespDTO> convertToRespDTO(List<AcademicClassDO> classDOList) {
        Map<Long, MajorDO> majorMap = majorMapper
                .listMajorByIds(classDOList.stream().map(AcademicClassDO::getMajorId).distinct().toList())
                .stream().collect(Collectors.toMap(MajorDO::getId, Function.identity()));
        Map<Long, GradeCohortDO> gradeMap = gradeCohortMapper
                .listGradeCohortByIds(classDOList.stream().map(AcademicClassDO::getGradeId).distinct().toList())
                .stream().collect(Collectors.toMap(GradeCohortDO::getId, Function.identity()));
        return classDOList.stream().map(each -> {
            AcademicClassRespDTO result = BeanUtil.copyProperties(each, AcademicClassRespDTO.class);
            result.setClassId(each.getId());
            MajorDO majorDO = majorMap.get(each.getMajorId());
            GradeCohortDO gradeCohortDO = gradeMap.get(each.getGradeId());
            result.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            result.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
            return result;
        }).toList();
    }
}
