package cc.cornerstones.biz.authentication.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.usermanagement.dto.UserAccountDto;
import cc.cornerstones.biz.authentication.dto.UserAuthenticationInstanceDto;
import com.alibaba.fastjson.JSONObject;

public interface UserAuthenticationService {
    UserAccountDto signIn(
            Long authenticationServiceAgentUid,
            JSONObject signInDto) throws AbcUndefinedException;

    void signOut(
            Long authenticationServiceAgentUid,
            Long userUid) throws AbcUndefinedException;

    UserAuthenticationInstanceDto createUserAuthenticationInstance(
            UserAccountDto userAccountDto) throws AbcUndefinedException;

    void revokeUserAuthenticationInstance(
            Long userUid) throws AbcUndefinedException;

    void validateAccessToken(
            String accessToken) throws AbcUndefinedException;

    void validateAccessToken(
            String accessToken, Long userUid) throws AbcUndefinedException;

    Long validateAccessTokenAndExtractUserUid(
            String accessToken) throws AbcUndefinedException;

    UserProfile validateAccessTokenAndRetrieveUserProfile(
            String accessToken) throws AbcUndefinedException;
}
