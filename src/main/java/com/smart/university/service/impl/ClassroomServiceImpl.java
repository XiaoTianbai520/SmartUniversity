package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.ClassroomDO;
import com.smart.university.domain.dto.req.ClassroomPageQueryReqDTO;
import com.smart.university.domain.dto.req.ClassroomSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.ClassroomRespDTO;
import com.smart.university.mapper.ClassroomMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.university.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教室服务实现
 */
@Service
@RequiredArgsConstructor
public class ClassroomServiceImpl extends ServiceImpl<ClassroomMapper, ClassroomDO> implements ClassroomService {

    private final ClassroomMapper classroomMapper;

    @Override
    public PageResult<ClassroomRespDTO> pageClassroom(ClassroomPageQueryReqDTO requestParam) {
        Page<ClassroomDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<ClassroomDO> pageResult = classroomMapper.listClassroomByCondition(page, requestParam);
        List<ClassroomDO> classroomDOList = pageResult.getRecords();
        List<ClassroomRespDTO> records = classroomDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public ClassroomRespDTO getClassroomDetail(Long classroomId) {
        ClassroomDO classroomDO = classroomMapper.getClassroomById(classroomId);
        if (classroomDO == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "教室不存在");
        }
        return convertToRespDTO(classroomDO);
    }

    @Override
    public Long saveClassroom(ClassroomSaveReqDTO requestParam) {
        ClassroomDO classroomDO = new ClassroomDO();
        classroomDO.setBuildingName(requestParam.getBuildingName());
        classroomDO.setRoomNo(requestParam.getRoomNo());
        classroomDO.setCapacity(requestParam.getCapacity());
        if (requestParam.getId() == null) {
            classroomDO.setStatus(1);
            classroomMapper.saveClassroom(classroomDO);
            return classroomDO.getId();
        }
        classroomDO.setId(requestParam.getId());
        classroomMapper.updateClassroom(classroomDO);
        return requestParam.getId();
    }

    @Override
    public void updateClassroomStatus(Long classroomId, StatusUpdateReqDTO requestParam) {
        ClassroomDO classroomDO = new ClassroomDO();
        classroomDO.setId(classroomId);
        classroomDO.setStatus(requestParam.getStatus());
        classroomMapper.updateClassroom(classroomDO);
    }

    private ClassroomRespDTO convertToRespDTO(ClassroomDO classroomDO) {
        ClassroomRespDTO result = BeanUtil.copyProperties(classroomDO, ClassroomRespDTO.class);
        result.setClassroomId(classroomDO.getId());
        return result;
    }
}
