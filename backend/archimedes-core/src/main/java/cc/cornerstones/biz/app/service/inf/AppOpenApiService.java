package cc.cornerstones.biz.app.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.app.dto.*;

public interface AppOpenApiService {
    AppOpenApiCredentialDto getOpenApiCredentialOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppOpenApiCredentialDto createOrReplaceOpenApiCredentialForApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppOpenApiAccountTypeDto getAccountTypeOfApp(
            Long appUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppOpenApiAccountTypeDto createOrReplaceAccountTypeForApp(
            Long appUid,
            CreateOrReplaceAppAccountTypeDto createOrReplaceAppAccountTypeDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    AppOpenApiAccountTypeDto getAccountTypeOfApp(
            String appKey,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
