package cc.cornerstones.biz.datafacet.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datafacet.dto.CreateDataFieldDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldAnotherDto;
import cc.cornerstones.biz.datafacet.dto.DataFieldDto;
import cc.cornerstones.biz.datafacet.dto.UpdateDataFieldDto;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datasource.service.assembly.database.DataColumnMetadata;
import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.*;

public interface DataFieldService {
    DataFieldDto createDataField(
            CreateDataFieldDto createDataFieldDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataFieldDto getDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDataField(
            Long dataFieldUid,
            UpdateDataFieldDto updateDataFieldDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataField(
            Long dataFieldUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void replaceAllDataFieldsOfDataFacet(
            Long dataFacetUid,
            List<DataFieldDto> dataFieldDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataFieldAnotherDto> pagingQueryDataFields(
            Long dataFacetUid,
            Long dataFieldUid,
            String dataFieldName,
            List<DataFieldTypeEnum> dataFieldTypeList,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataFieldAnotherDto> listingQueryDataFields(
            Long dataFacetUid,
            Long dataFieldUid,
            String dataFieldName,
            List<DataFieldTypeEnum> dataFieldTypeList,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void initDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void reinitDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void reinitDataFieldsOfDataFacetWithDataColumnDoList(
            Long dataFacetUid,
            List<DataColumnDo> dataColumnDoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteAllDataFieldsOfDataFacet(
            Long dataFacetUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
