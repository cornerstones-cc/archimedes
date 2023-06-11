package cc.cornerstones.biz.operations.migration.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.operations.migration.dto.MigrationDeploymentDto;
import cc.cornerstones.biz.operations.migration.dto.PrepareMigrateOutDto;
import cc.cornerstones.biz.operations.migration.dto.PreparedMigrateOutDto;
import cc.cornerstones.biz.operations.migration.dto.StartMigrateOutDto;

import java.nio.file.Path;

public interface MigrationService {
    PreparedMigrateOutDto prepareMigrateOut(
            PrepareMigrateOutDto prepareMigrateOutDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Path startMigrateOut(
            StartMigrateOutDto startMigrateOutDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long startMigrateIn(
            MigrationDeploymentDto migrationDeploymentDto,
            Path exportTemplatesDirectoryPath,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Long findTaskUidOfTheMigrateInTaskInProgress(
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
