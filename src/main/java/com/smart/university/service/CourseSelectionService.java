package com.smart.university.service;

import com.smart.university.domain.dto.req.CourseSelectReqDTO;
import com.smart.university.domain.dto.req.StudentSelectionQueryReqDTO;
import com.smart.university.domain.dto.resp.CourseSelectionRespDTO;
import com.smart.university.domain.dto.resp.StudentSelectionRespDTO;

import java.util.List;

/**
 * 学生选课服务，承载选课核心业务校验
 */
public interface CourseSelectionService {

    /**
     * 学生选课，按批次、专业、年级、重复选课、时间冲突、学分、容量顺序校验
     *
     * @param requestParam 选课入参
     * @return 选课结果
     */
    CourseSelectionRespDTO selectCourse(CourseSelectReqDTO requestParam);

    /**
     * 学生退课
     *
     * @param selectionId 选课记录 ID
     */
    void withdrawCourse(Long selectionId);

    /**
     * 查询我的课程
     *
     * @param requestParam 查询条件，学生 ID 与学期由后端填充
     * @return 我的课程集合
     */
    List<StudentSelectionRespDTO> listStudentSelection(StudentSelectionQueryReqDTO requestParam);
}
