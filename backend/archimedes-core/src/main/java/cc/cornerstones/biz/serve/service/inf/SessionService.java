package cc.cornerstones.biz.serve.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.serve.dto.CreateSessionDto;
import cc.cornerstones.biz.serve.dto.SessionDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface SessionService {
    SessionDto createSessionForDataWidget(
            Long dataFacetUid,
            CreateSessionDto createSessionDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteMySessionOfDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<SessionDto> listingQuerySessionsOfDataWidget(
            Long userUid,
            Long dataWidgetUid,
            Long uid,
            String name,
            List<String> lastModifiedTimestampAsStringList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
