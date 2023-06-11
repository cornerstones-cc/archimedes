package cc.cornerstones.biz.datatable.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.datatable.dto.CreateDataColumnDto;
import cc.cornerstones.biz.datatable.dto.DataColumnDto;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataColumnService {
    List<DataColumnDto> listingQueryDataColumns(
            Long dataTableUid,
            Long dataColumnUid,
            String dataColumnName,
            DataColumnTypeEnum dataColumnType,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    void createDataColumns(
            DataTableDo dataTableDo,
            List<CreateDataColumnDto> createDataColumnDtoList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
