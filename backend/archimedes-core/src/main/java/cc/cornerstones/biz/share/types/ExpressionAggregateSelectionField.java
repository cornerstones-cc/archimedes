package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.datasource.share.constants.DataColumnTypeEnum;
import cc.cornerstones.biz.share.constants.AggregateFunctionEnum;
import lombok.Data;

@Data
public class ExpressionAggregateSelectionField {
    private String sourceFieldName;
    private DataColumnTypeEnum sourceFieldType;
    private AggregateFunctionEnum aggregateFunction;
    private String targetFieldName;
}
