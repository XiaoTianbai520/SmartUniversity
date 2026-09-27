package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.entity.CourseDO;
import com.smart.university.domain.enums.CourseTypeEnum;
import com.smart.university.domain.dto.req.CoursePageQueryReqDTO;
import com.smart.university.domain.dto.req.CourseSaveReqDTO;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.resp.CourseRespDTO;
import com.smart.university.mapper.CourseMapper;
import com.smart.university.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程服务实现
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;

    @Override
    public PageResult<CourseRespDTO> pageCourse(CoursePageQueryReqDTO requestParam) {
        long total = courseMapper.countCourseByCondition(requestParam);
        if (total <= 0) {
            return PageResult.empty(requestParam.getCurrentPage(), requestParam.getLimit());
        }
        List<CourseDO> courseDOList = courseMapper.listCourseByCondition(requestParam);
        List<CourseRespDTO> records = courseDOList.stream().map(this::convertToRespDTO).toList();
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), total);
    }

    @Override
    public CourseRespDTO getCourseDetail(Long courseId) {
        return convertToRespDTO(getCourseById(courseId));
    }

    @Override
    public Long saveCourse(CourseSaveReqDTO requestParam) {
        CourseTypeEnum courseTypeEnum = CourseTypeEnum.getByCode(requestParam.getCourseType());
        if (courseTypeEnum == null) {
            throw new BizException(ResultCodeEnum.PARAM_ERROR, "课程性质不合法");
        }
        if (requestParam.getId() == null && courseMapper.countCourseByCourseCode(requestParam.getCourseCode()) > 0) {
            throw new BizException(ResultCodeEnum.COURSE_CODE_EXIST);
        }
        CourseDO existCourseDO = requestParam.getId() == null ? null : getCourseById(requestParam.getId());
        if (existCourseDO != null && ObjectUtil.notEqual(existCourseDO.getCourseCode(), requestParam.getCourseCode())
                && courseMapper.countCourseByCourseCode(requestParam.getCourseCode()) > 0) {
            throw new BizException(ResultCodeEnum.COURSE_CODE_EXIST);
        }
        CourseDO courseDO = new CourseDO();
        courseDO.setCourseCode(requestParam.getCourseCode());
        courseDO.setCourseName(requestParam.getCourseName());
        courseDO.setDescription(requestParam.getDescription());
        courseDO.setCredit(requestParam.getCredit());
        courseDO.setCourseType(courseTypeEnum);
        courseDO.setDefaultCapacity(requestParam.getDefaultCapacity());
        if (requestParam.getId() == null) {
            courseDO.setStatus(1);
            courseMapper.saveCourse(courseDO);
            return courseDO.getId();
        }
        courseDO.setId(requestParam.getId());
        courseMapper.updateCourse(courseDO);
        return requestParam.getId();
    }

    @Override
    public void updateCourseStatus(Long courseId, StatusUpdateReqDTO requestParam) {
        CourseDO courseDO = new CourseDO();
        courseDO.setId(courseId);
        courseDO.setStatus(requestParam.getStatus());
        courseMapper.updateCourse(courseDO);
    }

    @Override
    public CourseDO getCourseById(Long courseId) {
        CourseDO result = courseMapper.getCourseById(courseId);
        if (result == null) {
            throw new BizException(ResultCodeEnum.COURSE_NOT_EXIST);
        }
        return result;
    }

    @Override
    public List<CourseDO> listCourseByIds(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        return courseMapper.listCourseByIds(courseIds);
    }

    private CourseRespDTO convertToRespDTO(CourseDO courseDO) {
        CourseRespDTO result = BeanUtil.copyProperties(courseDO, CourseRespDTO.class);
        result.setCourseId(courseDO.getId());
        result.setCourseType(courseDO.getCourseType() == null ? null : courseDO.getCourseType().name());
        return result;
    }
}
