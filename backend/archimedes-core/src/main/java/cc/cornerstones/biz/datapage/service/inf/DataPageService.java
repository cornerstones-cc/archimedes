package cc.cornerstones.biz.datapage.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datapage.dto.CreateDataPageDto;
import cc.cornerstones.biz.datapage.dto.DataPageDto;
import cc.cornerstones.biz.datapage.dto.UpdateDataPageDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataPageService {
    DataPageDto createDataPage(
            CreateDataPageDto createDataPageDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void updateDataPage(
            Long dataPageUid,
            UpdateDataPageDto updateDataPageDto,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    DataPageDto getDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<String> listAllReferencesToDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void deleteDataPage(
            Long dataPageUid,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    Page<DataPageDto> pagingQueryDataPages(
            Long dataPageUid,
            String dataPageName,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    List<DataPageDto> listingQueryDataPages(
            Long dataPageUid,
            String dataPageName,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
