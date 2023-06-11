package cc.cornerstones.biz.datasource.share.types;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;

import java.util.List;

public interface RowHandler<T> {
    T process(List<List<Object>> rows, List<String> columnLabels) throws AbcUndefinedException;

    Object transformColumnValue(Object columnValue, String columnLabel);
}
