package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.entity.CourseSelectionDO;
import com.smart.university.domain.entity.SelectionBatchClassDO;
import com.smart.university.domain.entity.SelectionBatchDO;
import com.smart.university.domain.entity.TeachingClassDO;
import com.smart.university.domain.enums.SelectionBatchStatusEnum;
import com.smart.university.domain.enums.TeachingClassStatusEnum;
import com.smart.university.domain.dto.req.BatchTeachingClassSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchPageQueryReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchSaveReqDTO;
import com.smart.university.domain.dto.req.SelectionBatchStatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.SelectionBatchRespDTO;
import com.smart.university.domain.dto.resp.TeachingClassRespDTO;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.mapper.CourseSelectionMapper;
import com.smart.university.mapper.SelectionBatchClassMapper;
import com.smart.university.mapper.SelectionBatchMapper;
import com.smart.university.mapper.TeachingClassMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.SelectionBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 选课批次服务实现
 */
@Service
@RequiredArgsConstructor
public class SelectionBatchServiceImpl extends ServiceImpl<SelectionBatchMapper, SelectionBatchDO> implements SelectionBatchService {

    private final SelectionBatchMapper selectionBatchMapper;

    private final SelectionBatchClassMapper selectionBatchClassMapper;

    private final TeachingClassMapper teachingClassMapper;

    private final CourseSelectionMapper courseSelectionMapper;

    private final CourseMapper courseMapper;

    @Override
    public PageResult<SelectionBatchRespDTO> pageSelectionBatch(SelectionBatchPageQueryReqDTO requestParam) {
        Page<SelectionBatchDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<SelectionBatchDO> pageResult = selectionBatchMapper.listSelectionBatchByCondition(page, requestParam);
        List<SelectionBatchDO> batchDOList = pageResult.getRecords();
        List<SelectionBatchRespDTO> records = batchDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public SelectionBatchRespDTO getSelectionBatchDetail(Long batchId) {
        return convertToRespDTO(getSelectionBatchById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveSelectionBatch(SelectionBatchSaveReqDTO requestParam) {
        if (!requestParam.getStartTime().isBefore(requestParam.getEndTime())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "选课开始时间必须早于结束时间");
        }
        if (requestParam.getDropDeadline() != null
                && requestParam.getDropDeadline().isBefore(requestParam.getStartTime())) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "退课截止时间不能早于选课开始时间");
        }
        SelectionBatchDO batchDO = new SelectionBatchDO();
        batchDO.setBatchName(requestParam.getBatchName());
        batchDO.setSemesterId(requestParam.getSemesterId());
        batchDO.setStartTime(requestParam.getStartTime());
        batchDO.setEndTime(requestParam.getEndTime());
        batchDO.setDropDeadline(requestParam.getDropDeadline());
        if (requestParam.getId() == null) {
            batchDO.setStatus(SelectionBatchStatusEnum.NOT_STARTED);
            selectionBatchMapper.saveSelectionBatch(batchDO);
            return batchDO.getId();
        }
        batchDO.setId(requestParam.getId());
        selectionBatchMapper.updateSelectionBatch(batchDO);
        return requestParam.getId();
    }

    @Override
    public void updateSelectionBatchStatus(Long batchId, SelectionBatchStatusUpdateReqDTO requestParam) {
        SelectionBatchStatusEnum statusEnum = SelectionBatchStatusEnum.getByCode(requestParam.getStatus());
        if (statusEnum == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "批次状态不合法");
        }
        SelectionBatchDO batchDO = new SelectionBatchDO();
        batchDO.setId(batchId);
        batchDO.setStatus(statusEnum);
        selectionBatchMapper.updateSelectionBatch(batchDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchTeachingClass(Long batchId, BatchTeachingClassSaveReqDTO requestParam) {
        SelectionBatchDO batchDO = getSelectionBatchById(batchId);
        for (Long each : requestParam.getTeachingClassIds()) {
            TeachingClassDO teachingClassDO = teachingClassMapper.getTeachingClassById(each);
            if (teachingClassDO == null) {
                throw new BizException(ResultCodeEnum.TEACHING_CLASS_NOT_EXIST);
            }
            if (!teachingClassDO.getSemesterId().equals(batchDO.getSemesterId())) {
                throw new BizException(ResultCodeEnum.PARAM_ERROR, "教学班与选课批次必须属于同一学期");
            }
            if (TeachingClassStatusEnum.CANCELLED == teachingClassDO.getStatus()) {
                throw new BizException(ResultCodeEnum.PARAM_ERROR, "已取消的教学班不能加入批次");
            }
            SelectionBatchClassDO param = new SelectionBatchClassDO();
            param.setBatchId(batchId);
            param.setTeachingClassId(each);
            if (selectionBatchClassMapper.countSelectionBatchClass(param) > 0) {
                continue;
            }
            selectionBatchClassMapper.saveSelectionBatchClass(param);
        }
    }

    @Override
    public List<TeachingClassRespDTO> listBatchTeachingClass(Long batchId) {
        List<Long> teachingClassIds = selectionBatchClassMapper.listTeachingClassIdByBatchId(batchId);
        if (CollUtil.isEmpty(teachingClassIds)) {
            return List.of();
        }
        List<TeachingClassDO> teachingClassDOList = teachingClassMapper.listTeachingClassByIds(teachingClassIds);
        Map<Long, CourseDO> courseMap = courseMapper
                .listCourseByIds(teachingClassDOList.stream().map(TeachingClassDO::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(CourseDO::getId, Function.identity()));
        return teachingClassDOList.stream().map(each -> {
            TeachingClassRespDTO result = BeanUtil.copyProperties(each, TeachingClassRespDTO.class);
            result.setTeachingClassId(each.getId());
            CourseDO courseDO = courseMap.get(each.getCourseId());
            result.setCourseName(courseDO == null ? null : courseDO.getCourseName());
            result.setStatus(each.getStatus() == null ? null : each.getStatus().name());
            result.setSelectedCount(courseSelectionMapper.countSelectedByTeachingClassId(each.getId()));
            return result;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatchTeachingClass(Long batchId, Long teachingClassId) {
        CourseSelectionDO selectionParam = new CourseSelectionDO();
        selectionParam.setBatchId(batchId);
        selectionParam.setTeachingClassId(teachingClassId);
        if (courseSelectionMapper.countSelectionByCondition(selectionParam) > 0) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "该教学班已存在选课记录，禁止移除");
        }
        SelectionBatchClassDO param = new SelectionBatchClassDO();
        param.setBatchId(batchId);
        param.setTeachingClassId(teachingClassId);
        selectionBatchClassMapper.removeSelectionBatchClass(param);
    }

    @Override
    public SelectionBatchDO getCurrentSelectionBatch(Long semesterId) {
        SelectionBatchDO result = selectionBatchMapper.getCurrentSelectionBatch(semesterId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.NOT_IN_SELECTION_TIME);
        }
        return result;
    }

    @Override
    public SelectionBatchDO getSelectionBatchById(Long batchId) {
        SelectionBatchDO result = selectionBatchMapper.getSelectionBatchById(batchId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.SELECTION_BATCH_NOT_EXIST);
        }
        return result;
    }

    private SelectionBatchRespDTO convertToRespDTO(SelectionBatchDO batchDO) {
        SelectionBatchRespDTO result = BeanUtil.copyProperties(batchDO, SelectionBatchRespDTO.class);
        result.setBatchId(batchDO.getId());
        result.setStatus(batchDO.getStatus() == null ? null : batchDO.getStatus().name());
        return result;
    }
}
