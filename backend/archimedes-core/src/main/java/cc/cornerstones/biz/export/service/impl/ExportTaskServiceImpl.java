package cc.cornerstones.biz.export.service.impl;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import cc.cornerstones.biz.distributedtask.persistence.DistributedTaskRepository;
import cc.cornerstones.biz.export.dto.ExportTaskDto;
import cc.cornerstones.biz.export.entity.ExportTaskDo;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.export.service.inf.ExportTaskService;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class ExportTaskServiceImpl implements ExportTaskService {

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    @Autowired
    private DistributedTaskRepository distributedTaskRepository;

    @Override
    public ExportTaskDto getExportTask(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            // 如果还没有创建 export task，则查看原始 task，不能直接报告 resource not found
            DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(taskUid);
            if (distributedTaskDo == null) {
                throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL,
                        taskUid));
            }

            ExportTaskDto exportTaskDto = new ExportTaskDto();
            BeanUtils.copyProperties(distributedTaskDo, exportTaskDto);
            exportTaskDto.setTaskUid(taskUid);
            exportTaskDto.setTaskName(distributedTaskDo.getName());
            exportTaskDto.setTaskStatus(ExportTaskStatusEnum.INITIALIZING);
            return exportTaskDto;
        } else {
            ExportTaskDto exportTaskDto = new ExportTaskDto();
            BeanUtils.copyProperties(exportTaskDo, exportTaskDto);
            return exportTaskDto;
        }
    }

    @Override
    public ExportTaskDto cancelExportTask(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        ExportTaskDo exportTaskDo = this.exportTaskRepository.findByTaskUid(taskUid);
        if (exportTaskDo == null) {
            // 如果还没有创建 export task，则查看原始 task，不能直接报告 resource not found
            DistributedTaskDo distributedTaskDo = this.distributedTaskRepository.findByUid(taskUid);
            if (distributedTaskDo == null) {
                throw new AbcResourceNotFoundException(String.format("%s::task_uid=%d", ExportTaskDo.RESOURCE_SYMBOL,
                        taskUid));
            }

            distributedTaskDo.setStatus(TaskStatusEnum.CANCELED);
            distributedTaskDo.setEndTimestamp(LocalDateTime.now());
            BaseDo.update(distributedTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
            this.distributedTaskRepository.save(distributedTaskDo);

            ExportTaskDto exportTaskDto = new ExportTaskDto();
            BeanUtils.copyProperties(distributedTaskDo, exportTaskDto);
            exportTaskDto.setTaskUid(taskUid);
            exportTaskDto.setTaskName(distributedTaskDo.getName());
            exportTaskDto.setTaskStatus(ExportTaskStatusEnum.CANCELED);
            return exportTaskDto;
        } else {
            switch (exportTaskDo.getTaskStatus()) {
                case INITIALIZING:
                case CREATED:
                case COUNTING:
                case QUERYING:
                case FETCHING:
                case TRANSFERRING:
                    exportTaskDo.setTaskStatus(ExportTaskStatusEnum.CANCELLING);
                    BaseDo.update(exportTaskDo, operatingUserProfile.getUid(), LocalDateTime.now());
                    this.exportTaskRepository.save(exportTaskDo);
                    break;
            }
            ExportTaskDto exportTaskDto = new ExportTaskDto();
            BeanUtils.copyProperties(exportTaskDo, exportTaskDto);
            return exportTaskDto;
        }
    }

    @Override
    public List<ExportTaskDto> listingQueryExportTasks(
            Long taskUid,
            String taskName,
            List<ExportTaskStatusEnum> taskStatusList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        Specification<ExportTaskDo> specification = new Specification<ExportTaskDo>() {
            @Override
            public Predicate toPredicate(Root<ExportTaskDo> root, CriteriaQuery<?> query,
                                         CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (operatingUserProfile != null && operatingUserProfile.getUid() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get(BaseDo.OWNER_FIELD_NAME),
                            operatingUserProfile.getUid()));
                }

                if (taskUid != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("taskUid"), taskUid));
                }
                if (!ObjectUtils.isEmpty(taskName)) {
                    predicateList.add(criteriaBuilder.like(root.get("taskName"), "%" + taskName + "%"));
                }
                if (!CollectionUtils.isEmpty(taskStatusList)) {
                    CriteriaBuilder.In<ExportTaskStatusEnum> in =
                            criteriaBuilder.in(root.get("taskStatus"));
                    taskStatusList.forEach(type -> {
                        in.value(type);
                    });
                    predicateList.add(in);
                }

                predicateList.add(criteriaBuilder.between(root.get(BaseDo.CREATED_TIMESTAMP_FIELD_NAME),
                        LocalDateTime.now().minusDays(7), LocalDateTime.now()));

                return criteriaBuilder.and(predicateList.toArray(new Predicate[predicateList.size()]));
            }
        };

        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Order.desc(BaseDo.ID_FIELD_NAME));
        }

        PageRequest pageRequest = PageRequest.of(0, 50, sort);
        Page<ExportTaskDo> itemDoPage = this.exportTaskRepository.findAll(specification, pageRequest);
        if (itemDoPage.isEmpty()) {
            return null;
        }
        List<ExportTaskDto> itemDtoList = new LinkedList<>();
        itemDoPage.forEach(itemDo -> {
            ExportTaskDto itemDto = new ExportTaskDto();
            BeanUtils.copyProperties(itemDo, itemDto);
            itemDto.setCountBeginTimestamp(itemDto.getBeginTimestamp());
            itemDtoList.add(itemDto);
        });
        return itemDtoList;
    }
}
