package cc.cornerstones.biz.authentication.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.UserDto;
import cc.cornerstones.biz.administration.usermanagement.dto.UserSimplifiedDto;
import cc.cornerstones.biz.authentication.dto.*;

public interface OpenApiAuthService {
    OpenApiSignedInDto signIn(
            OpenApiSignInDto signInDto) throws AbcUndefinedException;

    RefreshedAccessTokenDto refreshAccessToken(
            RefreshAccessTokenDto refreshAccessTokenDto) throws AbcUndefinedException;

    void authenticate(
            String clientId,
            String accessToken) throws AbcUndefinedException;

    UserProfile getUserProfile(
            String clientId,
            String subject) throws AbcUndefinedException;

    void authorizeDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    UserDto getUser(
            String clientId,
            String subject) throws AbcUndefinedException;

    UserSimplifiedDto getUserSimple(
            String clientId,
            String subject) throws AbcUndefinedException;
}
