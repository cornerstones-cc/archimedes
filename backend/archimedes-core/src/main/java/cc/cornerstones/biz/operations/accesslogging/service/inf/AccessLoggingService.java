package cc.cornerstones.biz.operations.accesslogging.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.operations.accesslogging.dto.CreateOrUpdateQueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.dto.QueryLogDto;
import cc.cornerstones.biz.operations.accesslogging.dto.SimpleQueryLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccessLoggingService {
    void createQueryLog(
            CreateOrUpdateQueryLogDto createQueryLogDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateQueryLog(
            CreateOrUpdateQueryLogDto updateQueryLogDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<SimpleQueryLogDto> pagingQueryQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            List<Long> userUidListOfUser,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    QueryLogDto getQueryLog(
            String trackingSerialNumber,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<SimpleQueryLogDto>  pagingQuerySlowQueryLogs(
            String trackingSerialNumber,
            Long dataFacetUid,
            String dataFacetName,
            List<Long> userUidListOfUser,
            List<String> createdTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
