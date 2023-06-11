package cc.cornerstones.biz.share.types;

import cc.cornerstones.biz.share.constants.AggregateFunctionEnum;
import cc.cornerstones.biz.share.constants.GroupFilterOperatorEnum;
import lombok.Data;

import java.util.List;

/**
 * Specifies a search condition for a group or an aggregate
 */
@Data
public class GroupFilter {
    private String fieldName;
    private AggregateFunctionEnum aggregateFunction;
    private GroupFilterOperatorEnum operator;
    private List<String> operands;
}
