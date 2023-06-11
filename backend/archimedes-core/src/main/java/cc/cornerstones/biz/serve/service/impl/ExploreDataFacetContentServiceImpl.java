package cc.cornerstones.biz.serve.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.serve.dto.AdvancedQueryDto;
import cc.cornerstones.biz.serve.service.inf.ExploreDataFacetContentService;
import cc.cornerstones.biz.share.constants.ExportOptionEnum;
import cc.cornerstones.biz.export.dto.ExportTaskDto;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExploreDataFacetContentServiceImpl implements ExploreDataFacetContentService {
    @Override
    public Page<JSONObject> queryContent(
            Long dataFacetUid,
            Map<String, String[]> queryParameterMap,
            AdvancedQueryDto advancedQueryDto,
            Pageable pageable,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        return null;
    }

    @Override
    public ExportTaskDto exportContent(
            Long dataFacetUid,
            Map<String, String[]> queryParameterMap,
            AdvancedQueryDto advancedQueryDto,
            ExportOptionEnum exportOption,
            Sort sort,
            UserProfile operatingUserProfile) {
        return null;
    }
}
