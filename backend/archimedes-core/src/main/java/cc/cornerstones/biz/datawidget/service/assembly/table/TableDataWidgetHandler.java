package cc.cornerstones.biz.datawidget.service.assembly.table;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.biz.datafacet.persistence.*;
import cc.cornerstones.biz.datadictionary.service.inf.DictionaryService;
import cc.cornerstones.biz.datawidget.service.assembly.DataWidgetHandler;
import cc.cornerstones.biz.datawidget.share.constants.DataWidgetTypeEnum;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TableDataWidgetHandler implements DataWidgetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TableDataWidgetHandler.class);

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private DataFieldRepository dataFieldRepository;

    @Autowired
    private FilteringDataFieldRepository filteringDataFieldRepository;

    @Autowired
    private FilteringExtendedRepository filteringExtendedRepository;

    @Autowired
    private ListingDataFieldRepository listingDataFieldRepository;

    @Autowired
    private ListingExtendedRepository listingExtendedRepository;

    @Autowired
    private SortingDataFieldRepository sortingDataFieldRepository;

    @Autowired
    private ExportBasicRepository exportBasicRepository;

    @Autowired
    private AdvancedFeatureRepository advancedFeatureRepository;

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * Data widget type
     *
     * @return
     */
    @Override
    public DataWidgetTypeEnum type() {
        return DataWidgetTypeEnum.TABLE;
    }

    @Override
    public void validateBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException {
        // 目前不用校验
    }

    @Override
    public JSONObject adjustBuildCharacteristicsAccordingToDataFacet(
            JSONObject buildCharacteristics,
            Long dataFacetUid) throws AbcUndefinedException {
        return null;
    }

}
