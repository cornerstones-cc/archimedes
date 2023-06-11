package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.ExportBasicDto;
import cc.cornerstones.biz.datafacet.dto.CreateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.ExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.dto.UpdateExportExtendedTemplateDto;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataFacetExportService {

    ExportBasicDto getExportBasicOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceExportBasicOfDataFacet(
            Long dataFacetUid,
            ExportBasicDto exportBasicDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    ExportExtendedTemplateDto createExportExtendedTemplateForDataFacet(
            Long dataFacetUid,
            CreateExportExtendedTemplateDto createExportExtendedTemplateDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    ExportExtendedTemplateDto getExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateExportExtendedTemplate(
            Long uid,
            UpdateExportExtendedTemplateDto updateExportExtendedTemplateDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteExportExtendedTemplate(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<ExportExtendedTemplateDto> listingQueryExportExtendedTemplatesOfDataFacet(
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility,
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<ExportExtendedTemplateDto> pagingQueryExportExtendedTemplatesOfDataFacet(
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility,
            Long uid,
            String name,
            String description,
            Boolean enabled,
            List<Long> userUidListOfLastModifiedBy,
            List<String> lastModifiedTimestampAsStringList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

}
