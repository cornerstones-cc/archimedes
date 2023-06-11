package cc.cornerstones.biz.administration.usermanagement.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.CreateUserSchemaExtendedPropertyDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UpdateUserSchemaExtendedPropertyDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserSchemaExtendedPropertyDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface UserSchemaService {
    UserSchemaExtendedPropertyDto createExtendedProperty(
            CreateUserSchemaExtendedPropertyDto createUserSchemaExtendedPropertyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateExtendedProperty(
            Long uid,
            UpdateUserSchemaExtendedPropertyDto updateUserSchemaExtendedPropertyDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserSchemaExtendedPropertyDto getExtendedProperty(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<UserSchemaExtendedPropertyDto> listingQueryExtendedProperties(
            Long uid,
            String name,
            Boolean showInFilter,
            Boolean showInDetailedInformation,
            Boolean showInBriefInformation,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<UserSchemaExtendedPropertyDto> pagingQueryExtendedProperties(
            Long uid,
            String name,
            String description,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Boolean showInFilter,
            Boolean showInDetailedInformation,
            Boolean showInBriefInformation,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;


    String transformExtendedPropertyValueFromObjectToString(
            Object extendedPropertyValue,
            Long extendedPropertyUid) throws AbcUndefinedException;
}
