package cc.cornerstones.biz.datatable.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.datatable.dto.DataIndexDto;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface DataIndexService {
    List<DataIndexDto> listingQueryDataIndexesOfDataTable(
            Long dataTableUid,
            Sort sort,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
