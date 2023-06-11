package cc.cornerstones.biz.distributedtask.service.inf;

import cc.cornerstones.almond.constants.TaskStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.distributedtask.dto.CreateDistributedTaskDto;
import cc.cornerstones.biz.distributedtask.dto.DistributedTaskDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DistributedTaskService {
    DistributedTaskDto createTask(
            CreateDistributedTaskDto createDistributedTaskDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void startTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void stopTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DistributedTaskDto getTask(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DistributedTaskDto> pagingQueryTasks(
            Long uid,
            String name,
            List<TaskStatusEnum> statusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
