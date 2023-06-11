package cc.cornerstones.biz.app.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.app.dto.AppDto;
import cc.cornerstones.biz.app.dto.CreateAppDto;
import cc.cornerstones.biz.app.dto.UpdateAppDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface AppService {
    AppDto createApp(
            CreateAppDto createAppDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateApp(
            Long uid,
            UpdateAppDto updateAppDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppDto getApp(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<AppDto> listingQueryApps(
            Long uid,
            String name,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<AppDto> pagingQueryApps(
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
