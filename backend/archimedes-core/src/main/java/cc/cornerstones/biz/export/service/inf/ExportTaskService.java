package cc.cornerstones.biz.export.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.export.dto.ExportTaskDto;
import cc.cornerstones.biz.export.share.constants.ExportTaskStatusEnum;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface ExportTaskService {
    ExportTaskDto getExportTask(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
    
    ExportTaskDto cancelExportTask(
            Long taskUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<ExportTaskDto> listingQueryExportTasks(
            Long taskUid,
            String taskName,
            List<ExportTaskStatusEnum> taskStatusList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
