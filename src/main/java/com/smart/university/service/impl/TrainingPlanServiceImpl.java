package com.smart.university.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.university.common.base.PageResult;
import com.smart.university.common.enums.ResultCodeEnum;
import com.smart.university.common.exception.BizException;
import com.smart.university.domain.dto.req.StatusUpdateReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanPageQueryReqDTO;
import com.smart.university.domain.dto.req.TrainingPlanSaveReqDTO;
import com.smart.university.domain.dto.resp.TrainingPlanRespDTO;
import com.smart.university.domain.entity.GradeCohortDO;
import com.smart.university.domain.entity.MajorDO;
import com.smart.university.domain.entity.TrainingPlanDO;
import com.smart.university.mapper.GradeCohortMapper;
import com.smart.university.mapper.MajorMapper;
import com.smart.university.mapper.TrainingPlanMapper;
import com.smart.university.service.TrainingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 培养方案服务实现，绑定维度为「专业 + 入学年级」（Q11 决议 A）
 */
@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanMapper trainingPlanMapper;
    private final MajorMapper majorMapper;
    private final GradeCohortMapper gradeCohortMapper;

    @Override
    public PageResult<TrainingPlanRespDTO> pageTrainingPlan(TrainingPlanPageQueryReqDTO requestParam) {
        Page<TrainingPlanDO> page = Page.of(requestParam.getCurrentPage(), requestParam.getLimit());
        IPage<TrainingPlanDO> pageResult = trainingPlanMapper.listTrainingPlanByCondition(page, requestParam);
        List<TrainingPlanRespDTO> records = convertToRespDTOList(pageResult.getRecords());
        return new PageResult<>(records, requestParam.getCurrentPage(), requestParam.getLimit(), pageResult.getTotal());
    }

    @Override
    public TrainingPlanRespDTO getTrainingPlanDetail(Long planId) {
        TrainingPlanDO trainingPlanDO = trainingPlanMapper.getTrainingPlanById(planId);
        if (trainingPlanDO == null) {
            throw new BizException(ResultCodeEnum.TRAINING_PLAN_NOT_EXIST);
        }
        return convertToRespDTO(trainingPlanDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTrainingPlan(TrainingPlanSaveReqDTO requestParam) {
        checkMajorAndGradeExist(requestParam);
        checkCreditConfig(requestParam);
        TrainingPlanDO trainingPlanDO = buildTrainingPlanDO(requestParam);
        if (requestParam.getId() == null) {
            checkMajorGradeUnique(requestParam.getMajorId(), requestParam.getGradeId(), null);
            trainingPlanDO.setStatus(requestParam.getStatus() == null ? 1 : requestParam.getStatus());
            trainingPlanMapper.saveTrainingPlan(trainingPlanDO);
            return trainingPlanDO.getId();
        }
        TrainingPlanDO existTrainingPlanDO = trainingPlanMapper.getTrainingPlanById(requestParam.getId());
        if (existTrainingPlanDO == null) {
            throw new BizException(ResultCodeEnum.TRAINING_PLAN_NOT_EXIST);
        }
        checkMajorGradeUnique(requestParam.getMajorId(), requestParam.getGradeId(), requestParam.getId());
        trainingPlanDO.setId(requestParam.getId());
        if (trainingPlanDO.getStatus() == null) {
            trainingPlanDO.setStatus(existTrainingPlanDO.getStatus());
        }
        trainingPlanMapper.updateTrainingPlan(trainingPlanDO);
        return requestParam.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrainingPlanStatus(Long planId, StatusUpdateReqDTO requestParam) {
        TrainingPlanDO existTrainingPlanDO = trainingPlanMapper.getTrainingPlanById(planId);
        if (existTrainingPlanDO == null) {
            throw new BizException(ResultCodeEnum.TRAINING_PLAN_NOT_EXIST);
        }
        TrainingPlanDO trainingPlanDO = new TrainingPlanDO();
        trainingPlanDO.setId(planId);
        trainingPlanDO.setStatus(requestParam.getStatus());
        trainingPlanMapper.updateTrainingPlan(trainingPlanDO);
    }

    @Override
    public TrainingPlanRespDTO getEnabledTrainingPlan(Long majorId, Long gradeId) {
        if (majorId == null || gradeId == null) {
            return null;
        }
        TrainingPlanDO trainingPlanDO = trainingPlanMapper.getTrainingPlanByMajorAndGrade(majorId, gradeId);
        if (trainingPlanDO == null || !Integer.valueOf(1).equals(trainingPlanDO.getStatus())) {
            return null;
        }
        return convertToRespDTO(trainingPlanDO);
    }

    @Override
    public List<TrainingPlanRespDTO> listTrainingPlan(TrainingPlanPageQueryReqDTO requestParam) {
        return convertToRespDTOList(trainingPlanMapper.listTrainingPlanByCondition(requestParam));
    }

    /**
     * 校验专业与年级是否存在
     *
     * @param requestParam 培养方案入参
     */
    private void checkMajorAndGradeExist(TrainingPlanSaveReqDTO requestParam) {
        if (majorMapper.getMajorById(requestParam.getMajorId()) == null) {
            throw new BizException(ResultCodeEnum.MAJOR_NOT_EXIST);
        }
        if (gradeCohortMapper.getGradeCohortById(requestParam.getGradeId()) == null) {
            throw new BizException(ResultCodeEnum.GRADE_NOT_EXIST);
        }
    }

    /**
     * 校验学分配置：总学分大于 0，且三类学分之和不超过总学分
     *
     * @param requestParam 培养方案入参
     */
    private void checkCreditConfig(TrainingPlanSaveReqDTO requestParam) {
        BigDecimal totalCredit = defaultZero(requestParam.getTotalCredit());
        BigDecimal requiredCredit = defaultZero(requestParam.getRequiredCredit());
        BigDecimal majorElectiveCredit = defaultZero(requestParam.getMajorElectiveCredit());
        BigDecimal publicElectiveCredit = defaultZero(requestParam.getPublicElectiveCredit());
        boolean creditInvalid = totalCredit.compareTo(BigDecimal.ZERO) <= 0
                || requiredCredit.compareTo(BigDecimal.ZERO) < 0
                || majorElectiveCredit.compareTo(BigDecimal.ZERO) < 0
                || publicElectiveCredit.compareTo(BigDecimal.ZERO) < 0
                || requiredCredit.add(majorElectiveCredit).add(publicElectiveCredit).compareTo(totalCredit) > 0;
        if (creditInvalid) {
            throw new BizException(ResultCodeEnum.TRAINING_PLAN_CREDIT_INVALID);
        }
    }

    /**
     * 校验专业与年级组合是否已被占用
     *
     * @param majorId    专业 ID
     * @param gradeId    年级 ID
     * @param planId     当前培养方案 ID，新增时为 null
     */
    private void checkMajorGradeUnique(Long majorId, Long gradeId, Long planId) {
        TrainingPlanDO sameTrainingPlanDO = trainingPlanMapper.getTrainingPlanByMajorAndGrade(majorId, gradeId);
        if (sameTrainingPlanDO == null) {
            return;
        }
        if (planId != null && planId.equals(sameTrainingPlanDO.getId())) {
            return;
        }
        throw new BizException(ResultCodeEnum.TRAINING_PLAN_DUPLICATE);
    }

    /**
     * 构建培养方案数据对象
     *
     * @param requestParam 培养方案入参
     * @return 培养方案数据对象
     */
    private TrainingPlanDO buildTrainingPlanDO(TrainingPlanSaveReqDTO requestParam) {
        TrainingPlanDO result = new TrainingPlanDO();
        result.setPlanName(requestParam.getPlanName());
        result.setMajorId(requestParam.getMajorId());
        result.setGradeId(requestParam.getGradeId());
        result.setTotalCredit(defaultZero(requestParam.getTotalCredit()));
        result.setRequiredCredit(defaultZero(requestParam.getRequiredCredit()));
        result.setMajorElectiveCredit(defaultZero(requestParam.getMajorElectiveCredit()));
        result.setPublicElectiveCredit(defaultZero(requestParam.getPublicElectiveCredit()));
        result.setStatus(requestParam.getStatus());
        result.setRemark(requestParam.getRemark());
        return result;
    }

    /**
     * 空学分按 0 处理
     *
     * @param credit 学分
     * @return 非空学分
     */
    private BigDecimal defaultZero(BigDecimal credit) {
        return credit == null ? BigDecimal.ZERO : credit;
    }

    /**
     * 批量转换并补齐专业 / 年级名称
     *
     * @param trainingPlanDOList 培养方案数据对象集合
     * @return 培养方案出参集合
     */
    private List<TrainingPlanRespDTO> convertToRespDTOList(List<TrainingPlanDO> trainingPlanDOList) {
        if (trainingPlanDOList == null || trainingPlanDOList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> majorIds = trainingPlanDOList.stream().map(TrainingPlanDO::getMajorId).distinct().toList();
        List<Long> gradeIds = trainingPlanDOList.stream().map(TrainingPlanDO::getGradeId).distinct().toList();
        Map<Long, MajorDO> majorMap = majorMapper.listMajorByIds(majorIds).stream()
                .collect(Collectors.toMap(MajorDO::getId, Function.identity(), (first, second) -> first));
        Map<Long, GradeCohortDO> gradeMap = gradeCohortMapper.listGradeCohortByIds(gradeIds).stream()
                .collect(Collectors.toMap(GradeCohortDO::getId, Function.identity(), (first, second) -> first));
        return trainingPlanDOList.stream().map(each -> {
            TrainingPlanRespDTO result = BeanUtil.copyProperties(each, TrainingPlanRespDTO.class);
            result.setPlanId(each.getId());
            MajorDO majorDO = majorMap.get(each.getMajorId());
            result.setMajorName(majorDO == null ? null : majorDO.getMajorName());
            GradeCohortDO gradeCohortDO = gradeMap.get(each.getGradeId());
            result.setGradeName(gradeCohortDO == null ? null : gradeCohortDO.getGradeName());
            result.setEntryYear(gradeCohortDO == null ? null : gradeCohortDO.getEntryYear());
            return result;
        }).toList();
    }

    /**
     * 单个转换并补齐专业 / 年级名称
     *
     * @param trainingPlanDO 培养方案数据对象
     * @return 培养方案出参
     */
    private TrainingPlanRespDTO convertToRespDTO(TrainingPlanDO trainingPlanDO) {
        return convertToRespDTOList(List.of(trainingPlanDO)).get(0);
    }
}
