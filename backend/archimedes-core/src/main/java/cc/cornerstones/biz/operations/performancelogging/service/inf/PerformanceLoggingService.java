package cc.cornerstones.biz.operations.performancelogging.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PerformanceLoggingService {

    Page<SimpleQueryLogDto> pagingQuerySlowQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            Long userUid,
            String displayName,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
