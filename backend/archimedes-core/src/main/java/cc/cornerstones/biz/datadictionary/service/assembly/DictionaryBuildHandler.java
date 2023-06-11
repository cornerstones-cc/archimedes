package cc.cornerstones.biz.datadictionary.service.assembly;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcPagination;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryResult;
import cc.cornerstones.biz.datatable.share.constants.DictionaryBuildTypeEnum;
import com.alibaba.fastjson.JSONObject;

public interface DictionaryBuildHandler {
    /**
     * Type
     *
     * @return
     */
    DictionaryBuildTypeEnum type();

    void validate(JSONObject params) throws AbcUndefinedException;

    QueryResult execute(JSONObject params, AbcPagination pagination) throws AbcUndefinedException;
}
