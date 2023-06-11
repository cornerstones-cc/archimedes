package cc.cornerstones.biz.administration.systemsettings.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.administration.systemsettings.dto.*;
import cc.cornerstones.biz.settings.dto.SignInOptionDto;
import cc.cornerstones.biz.settings.dto.SystemProfileDto;

import java.util.List;

public interface SystemSettingsService {
    SystemReleaseDto createSystemRelease(
            CreateSystemReleaseDto createSystemReleaseDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    SystemReleaseDto getSystemRelease(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    SystemReleaseDto getLatestSystemRelease(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    ArchiveCleanupSettingsDto createOrReplaceArchiveCleanupSettings(
            CreateOrReplaceArchiveCleanupSettingsDto createOrReplaceArchiveCleanupSettingsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    ArchiveCleanupSettingsDto getArchiveCleanupSettings(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<SignInOptionDto> listingQuerySignInOptions() throws AbcUndefinedException;

    List<TreeNode> listingQueryNavigationMenus(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    CapacityControlSettingsDto createOrReplaceCapacityControlSettings(
            CreateOrReplaceCapacityControlSettingsDto createOrReplaceCapacityControlSettingsDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    CapacityControlSettingsDto getCapacityControlSettings(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
