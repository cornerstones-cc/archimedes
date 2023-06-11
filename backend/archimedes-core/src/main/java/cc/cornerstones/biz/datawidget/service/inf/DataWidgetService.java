package cc.cornerstones.biz.datawidget.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datawidget.dto.CreateDataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.DataWidgetDto;
import cc.cornerstones.biz.datawidget.dto.UpdateDataWidgetDto;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataWidgetService {
    DataWidgetDto createDataWidget(
            Long dataFacetUid,
            CreateDataWidgetDto createDataWidgetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDataWidget(
            Long uid,
            UpdateDataWidgetDto updateDataWidgetDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataWidgetDto getDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataWidget(
            Long uid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataWidgetDto> pagingQueryDataWidgets(
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataWidgetDto> listingQueryDataWidgets(
            Long uid,
            String name,
            DataWidgetTypeEnum type,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
