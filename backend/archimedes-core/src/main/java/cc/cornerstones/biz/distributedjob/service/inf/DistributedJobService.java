package cc.cornerstones.biz.distributedjob.service.inf;

import cc.cornerstones.almond.constants.JobStatusEnum;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.distributedjob.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DistributedJobService {
    DistributedJobDto createJob(
            CreateDistributedJobDto createDistributedJobDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateJob(
            Long jobUid,
            UpdateDistributedJobDto updateDistributedJobDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DistributedJobDto getJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteJob(
            Long jobUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DistributedJobDto> pagingQueryJobs(
            Long jobUid,
            String jobName,
            Boolean enabled,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DistributedJobExecutionDto startJobExecution(
            Long jobUid) throws AbcUndefinedException;

    DistributedJobExecutionDto stopJobExecution(
            Long jobExecutionUid) throws AbcUndefinedException;

    Page<DistributedJobExecutionDto> pagingQueryJobExecutions(
            Long jobUid,
            Long uid,
            List<JobStatusEnum> statusList,
            List<String> createdTimestampAsStringList,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
