package cc.cornerstones.biz.datasource.service.assembly.database.mssql;

import cc.cornerstones.almond.exceptions.AbcIllegalParameterException;
import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.*;
import cc.cornerstones.almond.utils.AbcMssqlUtils;
import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import cc.cornerstones.biz.datasource.dto.DataSourceQueryDto;
import cc.cornerstones.biz.datasource.service.assembly.database.QueryBuilder;
import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import cc.cornerstones.biz.share.constants.FilteringTypeEnum;
import cc.cornerstones.biz.share.types.*;
import com.alibaba.druid.wall.WallCheckResult;
import com.alibaba.druid.wall.spi.SQLServerWallProvider;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Component
public class MssqlQueryBuilder implements QueryBuilder {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MssqlQueryBuilder.class);

    /**
     * Database server type
     *
     * @return
     */
    @Override
    public DatabaseServerTypeEnum type() {
        return DatabaseServerTypeEnum.MSSQL;
    }

    @Override
    public void verifyStatement(
            String statement) throws AbcUndefinedException {
        // 分析 SQL 语义来防御 SQL 注入攻击
        SQLServerWallProvider provider = new SQLServerWallProvider();
        WallCheckResult checkResult = provider.check(statement);
        if (!checkResult.getViolations().isEmpty()) {
            StringBuilder logMsg = new StringBuilder();
            logMsg.append("sql=").append(statement).append(";");
            checkResult.getViolations().forEach(violation -> {
                if (logMsg.length() > 0) {
                    logMsg.append(";");
                }
                logMsg.append("error_code=").append(violation.getErrorCode())
                        .append(",message=").append(violation.getMessage());
            });
            throw new AbcIllegalParameterException(logMsg.toString());
        }
    }

    @Override
    public String buildCountStatement(
            DataSourceQueryDto queryLayout) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        //
        // step 1, select ... count ... from ...
        //
        String selectFromClause = buildSelectCountFromClause(
                queryLayout.getTableType(),
                queryLayout.getTableNameOfDirectTable(), queryLayout.getTableContextPathOfDirectTable(),
                queryLayout.getBuildingLogicOfIndirectTable());
        if (ObjectUtils.isEmpty(selectFromClause)) {
            throw new AbcResourceConflictException("fail to build sql");
        }
        sql.append(selectFromClause);

        //
        // step 2, where
        //
        String whereClause = buildWhereClause(
                queryLayout.getPlainFilters(),
                queryLayout.getCascadingFilters(),
                queryLayout.getDataPermissionFilters());
        if (!ObjectUtils.isEmpty(whereClause)) {
            sql.append(" WHERE").append(" ").append(whereClause);
        }
        String moreWhereClause = buildWhereClause(queryLayout.getStatementFilter());
        if (!ObjectUtils.isEmpty(moreWhereClause)) {
            if (ObjectUtils.isEmpty(whereClause)) {
                sql.append(" WHERE").append(" ").append(moreWhereClause);
            } else {
                sql.append(" AND").append("(").append(moreWhereClause).append(")");
            }
        }

        //
        // step 3, group by
        //
        String groupByClause = buildGroupByClause(queryLayout.getGroupByFields());
        if (!ObjectUtils.isEmpty(groupByClause)) {
            sql.append(" GROUP BY").append(" ").append(groupByClause);
        }

        //
        // step 4, having
        //
        String havingClause = buildHavingClause(queryLayout.getGroupFilters());
        if (!ObjectUtils.isEmpty(havingClause)) {
            sql.append(" HAVING").append(" ").append(havingClause);
        }

        //
        // step 5, verify statement
        //
        verifyStatement(sql.toString());

        return sql.toString();
    }

    @Override
    public String buildQueryStatement(
            DataSourceQueryDto queryLayout) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        //
        // step 1, select ... columns ... from ...
        //
        String selectFromClause = buildSelectColumnsFromClause(
                queryLayout.getTableType(),
                queryLayout.getTableNameOfDirectTable(), queryLayout.getTableContextPathOfDirectTable(),
                queryLayout.getBuildingLogicOfIndirectTable(),
                queryLayout.getSelectionFields());
        if (ObjectUtils.isEmpty(selectFromClause)) {
            throw new AbcResourceConflictException("fail to build sql");
        }
        sql.append(selectFromClause);

        //
        // step 2, where
        //
        String whereClause = buildWhereClause(
                queryLayout.getPlainFilters(),
                queryLayout.getCascadingFilters(),
                queryLayout.getDataPermissionFilters());
        if (!ObjectUtils.isEmpty(whereClause)) {
            sql.append(" WHERE").append(" ").append(whereClause);
        }
        String moreWhereClause = buildWhereClause(queryLayout.getStatementFilter());
        if (!ObjectUtils.isEmpty(moreWhereClause)) {
            if (ObjectUtils.isEmpty(whereClause)) {
                sql.append(" WHERE").append(" ").append(moreWhereClause);
            } else {
                sql.append(" AND").append("(").append(moreWhereClause).append(")");
            }
        }

        //
        // step 3, group by
        //
        String groupByClause = buildGroupByClause(queryLayout.getGroupByFields());
        if (!ObjectUtils.isEmpty(groupByClause)) {
            sql.append(" GROUP BY").append(" ").append(groupByClause);
        }

        //
        // step 4, having
        //
        String havingClause = buildHavingClause(queryLayout.getGroupFilters());
        if (!ObjectUtils.isEmpty(havingClause)) {
            sql.append(" HAVING").append(" ").append(havingClause);
        }

        //
        // step 5, sort (order by)
        //
        String sortClause = buildSortClause(queryLayout.getSort());
        if (!ObjectUtils.isEmpty(sortClause)) {
            sql.append(" ORDER BY").append(" ").append(sortClause);
        }

        //
        // step 6, pagination (limit)
        //
        String paginationClause = buildPaginationClause(queryLayout.getPagination());
        if (!ObjectUtils.isEmpty(paginationClause)) {
            sql.append(" ").append(paginationClause);
        }

        //
        // verify statement
        //
        try {
            verifyStatement(sql.toString());
        } catch (Exception e) {
            LOGGER.error("verify statement failed:{}", sql);
            throw e;
        }

        return sql.toString();
    }

    private String buildGroupByClause(List<GroupByField> groupByFields) {
        if (CollectionUtils.isEmpty(groupByFields)) {
            return null;
        }

        StringBuilder clause = new StringBuilder();
        groupByFields.forEach(groupByField -> {
            if (clause.length() > 0) {
                clause.append(",");
            }
            clause.append(AbcMssqlUtils.buildEscapedName(groupByField.getFieldName()));
        });

        return clause.toString();
    }

    private String buildHavingClause(List<GroupFilter> groupFilters) {
        if (CollectionUtils.isEmpty(groupFilters)) {
            return null;
        }

        StringBuilder clause = new StringBuilder();
        groupFilters.forEach(groupFilter -> {
            if (clause.length() > 0) {
                clause.append(" ").append("AND").append(" ");
            }

            switch (groupFilter.getAggregateFunction()) {
                case MAX: {
                    clause.append("MAX(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case MIN: {
                    clause.append("MIN(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case SUM: {
                    clause.append("SUM(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case VAR: {
                    clause.append("VAR_SAMP(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case Varp: {
                    clause.append("VAR_POP(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case COUNT: {
                    clause.append("COUNT(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case STDDEV: {
                    clause.append("STDDEV_SAMP(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case STDEVP: {
                    clause.append("STDDEV(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case AVERAGE: {
                    clause.append("AVG(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                case PRODUCT: {
                    // TODO
                }
                break;
                case COUNT_NUMBERS: {
                    clause.append("COUNT(" + AbcMssqlUtils.buildEscapedName(groupFilter.getFieldName()) + ")");
                }
                break;
                default:
                    throw new AbcUndefinedException(String.format("unsupported aggregate function:%s",
                            groupFilter.getAggregateFunction()));
            }

            switch (groupFilter.getOperator()) {
                case EQUALS:
                    clause.append(" = " + groupFilter.getOperands().get(0));
                    break;
                case DOES_NOT_EQUAL:
                    clause.append(" <> " + groupFilter.getOperands().get(0));
                    break;
                case GREATER_THAN:
                    clause.append(" > " + groupFilter.getOperands().get(0));
                    break;
                case GREATER_THAN_OR_EQUAL_TO:
                    clause.append(" >= " + groupFilter.getOperands().get(0));
                    break;
                case LESS_THAN:
                    clause.append(" < " + groupFilter.getOperands().get(0));
                    break;
                case LESS_THAN_OR_EQUAL_TO:
                    clause.append(" <= " + groupFilter.getOperands().get(0));
                    break;
                case BETWEEN: {
                    clause.append(" BETWEEN " + groupFilter.getOperands().get(0) + " AND " + groupFilter.getOperands().get(1));
                }
                break;
                case IS_NULL:
                    clause.append(" IS NULL");
                    break;
                case IS_NOT_NULL:
                    clause.append(" IS NOT NULL");
                    break;
                case ENDS_WITH:
                    clause.append(" LIKE " + "'%" + groupFilter.getOperands().get(0) + "'");
                    break;
                case DOES_NOT_END_WITH:
                    clause.append(" NOT LIKE " + "'%" + groupFilter.getOperands().get(0) + "'");
                    break;
                case BEGINS_WITH:
                    clause.append(" LIKE " + "'" + groupFilter.getOperands().get(0) + "%'");
                    break;
                case DOES_NOT_BEGIN_WITH:
                    clause.append(" NOT LIKE " + "'" + groupFilter.getOperands().get(0) + "%'");
                    break;
                case CONTAINS:
                    clause.append(" LIKE " + "'%" + groupFilter.getOperands().get(0) + "%'");
                    break;
                case DOES_NOT_CONTAIN:
                    clause.append(" NOT LIKE " + "'%" + groupFilter.getOperands().get(0) + "%'");
                    break;
                case IN: {
                    clause.append(" IN (");
                    for (int index = 0; index < groupFilter.getOperands().size(); index++) {
                        if (index > 0) {
                            clause.append(",").append(groupFilter.getOperands().get(index));
                        }
                    }
                    clause.append(")");
                }
                break;
            }
        });

        return clause.toString();
    }

    private String buildSelectCountFromClause(
            DataTableTypeEnum tableType,
            String tableName,
            List<String> tableContextPath,
            String buildingLogicOfIndirectTable) {
        StringBuilder clause = new StringBuilder();

        switch (tableType) {
            case DATABASE_TABLE:
            case DATABASE_VIEW: {
                if (CollectionUtils.isEmpty(tableContextPath) || tableContextPath.size() != 2) {
                    LOGGER.error("context path should contain database name and schema name");
                    return null;
                }

                // columns
                clause.append("SELECT COUNT(*) AS ").append(QueryBuilder.COUNT_COLUMN_NAME);
                clause.append(" FROM");
                String databaseName = tableContextPath.get(0);
                String schemaName = tableContextPath.get(1);
                clause.append(" ")
                        .append(AbcMssqlUtils.buildEscapedName(databaseName))
                        .append(".").append(AbcMssqlUtils.buildEscapedName(schemaName))
                        .append(".").append(AbcMssqlUtils.buildEscapedName(tableName));
            }
            break;
            case INDIRECT_TABLE: {
                String tableAlias = "ta";

                // columns
                clause.append("SELECT COUNT(*) AS ").append(QueryBuilder.COUNT_COLUMN_NAME);
                clause.append(" FROM");
                clause.append(" ")
                        .append("(")
                        .append(buildingLogicOfIndirectTable)
                        .append(")")
                        .append(" ").append(tableAlias);
            }
            break;
            default:
                break;
        }

        return clause.toString();
    }

    private String buildSelectColumnsFromClause(
            DataTableTypeEnum tableType,
            String tableName,
            List<String> tableContextPath,
            String buildingLogicOfIndirectTable,
            List<SelectionField> selectionFields) {
        StringBuilder clause = new StringBuilder();

        switch (tableType) {
            case DATABASE_TABLE:
            case DATABASE_VIEW: {
                if (CollectionUtils.isEmpty(tableContextPath) || tableContextPath.size() != 2) {
                    LOGGER.error("context path should contain database name and schema name");
                    return null;
                }

                // columns
                clause.append("SELECT");
                for (int i = 0; i < selectionFields.size(); i++) {
                    if (i == 0) {
                        clause.append(" ");
                    } else {
                        clause.append(",");
                    }

                    SelectionField selectionField = selectionFields.get(i);
                    switch (selectionField.getType()) {
                        case PLAIN: {
                            PlainSelectionField plainSelectionField =
                                    JSONObject.toJavaObject(selectionField.getContent(), PlainSelectionField.class);
                            String fieldName = plainSelectionField.getFieldName();

                            clause.append(AbcMssqlUtils.buildEscapedName(fieldName));
                        }
                        break;
                        case EXPRESSION: {
                            ExpressionSelectionField expressionSelectionField =
                                    JSONObject.toJavaObject(selectionField.getContent(),
                                            ExpressionSelectionField.class);
                            switch (expressionSelectionField.getType()) {
                                case AGGREGATE_FUNCTION: {
                                    ExpressionAggregateSelectionField expressionAggregateSelectionField =
                                            JSONObject.toJavaObject(expressionSelectionField.getContent(),
                                                    ExpressionAggregateSelectionField.class);
                                    String fieldName =
                                            buildExpressionAggregateSelectionField(expressionAggregateSelectionField);

                                    clause.append(fieldName);
                                }
                                break;
                                case PLACEHOLDER: {

                                }
                                break;
                                default:
                                    throw new AbcUndefinedException(String.format("unsupported " +
                                                    "expression selection field type:%s",
                                            expressionSelectionField.getType()));
                            }
                        }
                        break;
                        default:
                            throw new AbcUndefinedException(String.format("unsupported selection field type:%s",
                                    selectionField.getType()));
                    }
                }

                clause.append(" FROM");
                String databaseName = tableContextPath.get(0);
                String schemaName = tableContextPath.get(1);
                clause.append(" ")
                        .append(AbcMssqlUtils.buildEscapedName(databaseName))
                        .append(".").append(AbcMssqlUtils.buildEscapedName(schemaName))
                        .append(".").append(AbcMssqlUtils.buildEscapedName(tableName));
            }
            break;
            case INDIRECT_TABLE: {
                String tableAlias = "ta";

                // columns
                clause.append("SELECT");
                for (int i = 0; i < selectionFields.size(); i++) {
                    if (i == 0) {
                        clause.append(" ");
                    } else {
                        clause.append(",");
                    }

                    SelectionField selectionField = selectionFields.get(i);
                    switch (selectionField.getType()) {
                        case PLAIN: {
                            PlainSelectionField plainSelectionField =
                                    JSONObject.toJavaObject(selectionField.getContent(), PlainSelectionField.class);
                            String fieldName = plainSelectionField.getFieldName();

                            clause.append(AbcMssqlUtils.buildEscapedName(tableAlias))
                                    .append(".")
                                    .append(AbcMssqlUtils.buildEscapedName(fieldName))
                                    .append(" AS ")
                                    .append(AbcMssqlUtils.buildEscapedName(fieldName));
                        }
                        break;
                        case EXPRESSION: {
                            ExpressionSelectionField expressionSelectionField =
                                    JSONObject.toJavaObject(selectionField.getContent(),
                                            ExpressionSelectionField.class);
                            switch (expressionSelectionField.getType()) {
                                case AGGREGATE_FUNCTION: {
                                    ExpressionAggregateSelectionField expressionAggregateSelectionField =
                                            JSONObject.toJavaObject(expressionSelectionField.getContent(),
                                                    ExpressionAggregateSelectionField.class);
                                    String fieldName =
                                            buildExpressionAggregateSelectionField(expressionAggregateSelectionField);

                                    clause.append(AbcMssqlUtils.buildEscapedName(tableAlias))
                                            .append(".")
                                            .append(AbcMssqlUtils.buildEscapedName(fieldName))
                                            .append(" AS ")
                                            .append(AbcMssqlUtils.buildEscapedName(fieldName));
                                }
                                break;
                                case PLACEHOLDER: {

                                }
                                break;
                                default:
                                    throw new AbcUndefinedException(String.format("unsupported " +
                                                    "expression selection field type:%s",
                                            expressionSelectionField.getType()));
                            }
                        }
                        break;
                        default:
                            throw new AbcUndefinedException(String.format("unsupported selection field type:%s",
                                    selectionField.getType()));
                    }
                }

                clause.append(" FROM");
                clause.append(" ")
                        .append("(")
                        .append(buildingLogicOfIndirectTable)
                        .append(")")
                        .append(" ").append(tableAlias);
            }
            break;
            default:
                break;
        }

        return clause.toString();
    }

    private String buildExpressionAggregateSelectionField(ExpressionAggregateSelectionField field) {
        switch (field.getAggregateFunction()) {
            case MAX: {
                return "MAX(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case MIN: {
                return "MIN(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case SUM: {
                switch (field.getSourceFieldType()) {
                    case TINYINT:
                    case SMALLINT:
                    case MEDIUMINT:
                        return "SUM(cast(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + " as int)" +
                                ")" + " " + "AS " + AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    case INT:
                        return "SUM(cast(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + " as bigint)"
                                + ")" + " " + "AS " + AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    case LONG:
                    case DECIMAL:
                        return "SUM(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                                AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    default:
                        return "''" + " AS " + AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                }
            }
            case VAR: {
                return "VAR_SAMP(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case Varp: {
                return "VAR_POP(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case COUNT: {
                return "COUNT(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case STDDEV: {
                return "STDDEV_SAMP(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case STDEVP: {
                return "STDDEV(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            }
            case AVERAGE: {
                switch (field.getSourceFieldType()) {
                    case TINYINT:
                    case SMALLINT:
                    case MEDIUMINT:
                        return "AVG(cast(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + " as int)"
                                + ")" + " " + "AS "
                                + AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    case INT:
                        return "AVG(cast(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + " as bigint)"
                                + ")" + " AS " +
                                AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    case LONG:
                    case DECIMAL:
                        return "AVG(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                                AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                    default:
                        return "''" + " AS " + AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
                }
            }
            case PRODUCT:
                // TODO
                break;
            case COUNT_NUMBERS:
                return "COUNT(" + AbcMssqlUtils.buildEscapedName(field.getSourceFieldName()) + ")" + " AS " +
                        AbcMssqlUtils.buildEscapedName(field.getTargetFieldName());
            default:
                throw new AbcUndefinedException(String.format("unsupported aggregate function:%s",
                        field.getAggregateFunction()));
        }
        return null;
    }

    private String buildWhereClause(StatementFilter statementFilter) {
        if (ObjectUtils.isEmpty(statementFilter)) {
            return null;
        }

        if (ObjectUtils.isEmpty(statementFilter.getStatement())) {
            return null;
        }

        return statementFilter.getStatement();
    }

    private String buildWhereClause(
            List<PlainFilter> plainFilters,
            List<CascadingFilter> cascadingFilters,
            List<DataPermissionFilter> dataPermissionFilters) {
        //
        // plain filters
        //
        StringBuilder clause10 = new StringBuilder();
        if (!CollectionUtils.isEmpty(plainFilters)) {
            plainFilters.forEach(filter -> {
                AbcTuple4<String, String[], FilteringTypeEnum, DataFieldTypeEnum> content = filter.getContent();

                String parameterName = content.f;
                String[] arrayOfParameterValue = content.s;
                FilteringTypeEnum filterType = content.t;
                DataFieldTypeEnum fieldType = content.u;

                if (arrayOfParameterValue.length == 0) {
                    return;
                }

                if (clause10.length() > 0) {
                    clause10.append(" ").append("AND").append(" ");
                }

                switch (filterType) {
                    case CONTAINS_TEXT:
                        clause10.append(buildFullFuzzyTextParameter(parameterName, arrayOfParameterValue));
                        break;
                    case ENDS_WITH_TEXT:
                        clause10.append(buildLeftFuzzyTextParameter(parameterName, arrayOfParameterValue));
                        break;
                    case BEGINS_WITH_TEXT:
                        clause10.append(buildRightFuzzyTextParameter(parameterName, arrayOfParameterValue));
                        break;
                    case EQUALS_TEXT:
                        clause10.append(buildExactTextParameter(parameterName, arrayOfParameterValue));
                        break;
                    case NUMBER_RANGE:
                        clause10.append(buildNumberRangeParameter(parameterName, arrayOfParameterValue));
                        break;
                    case DATE_RANGE:
                        clause10.append(buildDateRangeParameter(parameterName, arrayOfParameterValue));
                        break;
                    case TIME_RANGE:
                        clause10.append(buildTimeRangeParameter(parameterName, arrayOfParameterValue));
                        break;
                    case DATETIME_RANGE:
                        clause10.append(buildDateTimeRangeParameter(parameterName, arrayOfParameterValue));
                        break;
                    case IS_NULL:
                        clause10.append(buildIsNullParameter(parameterName));
                        break;
                    case IS_NOT_NULL:
                        clause10.append(buildIsNotNullParameter(parameterName));
                        break;
                    default: {
                        switch (fieldType) {
                            case STRING:
                            case DATE:
                            case DATETIME:
                            case TIME:
                            case FILE:
                            case IMAGE:
                                clause10.append(buildExactTextParameter(parameterName, arrayOfParameterValue));
                                break;
                            default:
                                clause10.append(buildExactNonTextParameter(parameterName, arrayOfParameterValue));
                                break;
                        }
                    }
                    break;
                }
            });
        }

        //
        // cascading filters
        //
        StringBuilder clause20 = new StringBuilder();
        if (!CollectionUtils.isEmpty(cascadingFilters)) {
            // cascading filter 之间是 AND 关系
            cascadingFilters.forEach(filter -> {
                // 每个 cascading filter 的第1级是 OR 关系，第2级是 AND 关系
                List<List<AbcTuple3<String, String[], DataFieldTypeEnum>>> content = filter.getContent();

                if (clause20.length() > 0) {
                    clause20.append(" AND");
                }

                StringBuilder clause21 = new StringBuilder();
                for (List<AbcTuple3<String, String[], DataFieldTypeEnum>> item : content) {
                    if (clause21.length() > 0) {
                        clause21.append(" OR ");
                    }

                    StringBuilder clause22 = new StringBuilder();
                    for (AbcTuple3<String, String[], DataFieldTypeEnum> tuple : item) {
                        String parameterName = tuple.f;
                        String[] parameterValue = tuple.s;
                        DataFieldTypeEnum fieldType = tuple.t;

                        if (clause22.length() > 0) {
                            clause22.append(" AND ");
                        }

                        switch (fieldType) {
                            case INTEGER:
                            case LONG:
                            case DECIMAL:
                            case BOOLEAN:
                                clause22.append(buildExactNonTextParameter(parameterName, parameterValue));
                                break;
                            default:
                                clause22.append(buildExactTextParameter(parameterName, parameterValue));
                                break;
                        }
                    }

                    if (clause22.length() > 0) {
                        clause21.append(" (").append(clause22).append(")");
                    }
                }

                if (clause21.length() > 0) {
                    clause20.append(" (").append(clause21).append(")");
                }
            });
        }

        //
        // data permission filters
        //
        StringBuilder clause30 = new StringBuilder();
        if (!CollectionUtils.isEmpty(dataPermissionFilters)) {
            // data permission filter 之间是 AND 关系
            dataPermissionFilters.forEach(filter -> {
                // 每个 data permission filter 的第1级是 OR 关系，第2级是 AND 关系
                List<List<AbcTuple3<String, String, DataFieldTypeEnum>>> content = filter.getContent();

                if (clause30.length() > 0) {
                    clause30.append(" AND");
                }

                StringBuilder clause31 = new StringBuilder();
                for (List<AbcTuple3<String, String, DataFieldTypeEnum>> item : content) {
                    if (clause31.length() > 0) {
                        clause31.append(" OR ");
                    }

                    StringBuilder clause32 = new StringBuilder();
                    for (AbcTuple3<String, String, DataFieldTypeEnum> tuple : item) {
                        String parameterName = tuple.f;
                        String parameterValue = tuple.s;
                        DataFieldTypeEnum fieldType = tuple.t;

                        if (clause32.length() > 0) {
                            clause32.append(" AND ");
                        }

                        switch (fieldType) {
                            case INTEGER:
                            case LONG:
                            case DECIMAL:
                            case BOOLEAN:
                                clause32.append(buildExactNonTextParameter(parameterName, parameterValue));
                                break;
                            default:
                                clause32.append(buildExactTextParameter(parameterName, parameterValue));
                                break;
                        }
                    }

                    if (clause32.length() > 0) {
                        clause31.append(" (").append(clause32).append(")");
                    }
                }

                if (clause31.length() > 0) {
                    clause30.append(" (").append(clause31).append(")");
                }
            });
        }

        StringBuilder clause = new StringBuilder();
        if (clause10.length() > 0) {
            clause.append(clause10);
        }
        if (clause20.length() > 0) {
            if (clause.length() > 0) {
                clause.append(" AND (").append(clause20).append(")");
            } else {
                clause.append(clause20);
            }
        }
        if (clause30.length() > 0) {
            if (clause.length() > 0) {
                clause.append(" AND (").append(clause30).append(")");
            } else {
                clause.append(clause30);
            }
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的数字范围条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildNumberRangeParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ")
                    .append(arrayOfParameterValue[0]);
        } else if (arrayOfParameterValue.length == 2) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" BETWEEN ")
                    .append(arrayOfParameterValue[0])
                    .append(" AND ")
                    .append(arrayOfParameterValue[1]);
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(arrayOfParameterValue[i]);
                } else {
                    clause.append(",").append(arrayOfParameterValue[i]);
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的文本精确匹配条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildExactTextParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ").append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[0])));
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i])));
                } else {
                    clause.append(",").append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i])));
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的文本精确匹配条件部分
     *
     * @param parameterName
     * @param parameterValue
     * @return
     */
    private String buildExactTextParameter(String parameterName, String parameterValue) {
        StringBuilder clause = new StringBuilder();

        clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                .append(" = ").append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(parameterValue)));

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的非文本精确匹配条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildExactNonTextParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ").append(arrayOfParameterValue[0]);
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(arrayOfParameterValue[i]);
                } else {
                    clause.append(",").append(arrayOfParameterValue[i]);
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的非文本精确匹配条件部分
     *
     * @param parameterName
     * @param parameterValue
     * @return
     */
    private String buildExactNonTextParameter(String parameterName, String parameterValue) {
        StringBuilder clause = new StringBuilder();

        clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                .append(" = ").append(parameterValue);

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的文本全模糊匹配条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildFullFuzzyTextParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" LIKE ")
                    .append("'%")
                    .append(arrayOfParameterValue[0])
                    .append("%'");
        } else {
            clause.append("(");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes("%" + AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i]) + "%"));
                } else {
                    clause.append(" OR ")
                            .append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes("%" + AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i]) + "%"));
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的文本左模糊匹配条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildLeftFuzzyTextParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" LIKE ")
                    .append("'%")
                    .append(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[0]))
                    .append("'");
        } else {
            clause.append("(");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes("%" + AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i])));
                } else {
                    clause.append(" OR ")
                            .append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes("%" + AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i])));
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的文本右模糊匹配条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildRightFuzzyTextParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" LIKE ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[0]) + "%"));
        } else {
            clause.append("(");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i]) + "%"));
                } else {
                    clause.append(" OR ")
                            .append(AbcMssqlUtils.buildEscapedName(parameterName))
                            .append(" LIKE ")
                            .append(AbcMssqlUtils.surroundBySingleQuotes(AbcMssqlUtils.buildEscapedValue(arrayOfParameterValue[i]) + "%"));
                }
            }
            clause.append(")");
        }

        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的日期范围条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue 前面的值小，后面的值大
     * @return
     */
    private String buildDateRangeParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0]));
        } else if (arrayOfParameterValue.length == 2) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" BETWEEN ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0] + " 00:00:00"))
                    .append(" AND ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[1] + " 23:59:59"));
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                } else {
                    clause.append(",").append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                }
            }
            clause.append(")");
        }
        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的时间范围条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildTimeRangeParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0]));
        } else if (arrayOfParameterValue.length == 2) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" BETWEEN ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0]))
                    .append(" AND ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[1]));
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                } else {
                    clause.append(",").append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                }
            }
            clause.append(")");
        }
        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的日期时间范围条件部分
     *
     * @param parameterName
     * @param arrayOfParameterValue
     * @return
     */
    private String buildDateTimeRangeParameter(String parameterName, String[] arrayOfParameterValue) {
        StringBuilder clause = new StringBuilder();
        if (arrayOfParameterValue.length == 1) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" = ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0]));
        } else if (arrayOfParameterValue.length == 2) {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" BETWEEN ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[0]))
                    .append(" AND ")
                    .append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[1]));
        } else {
            clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                    .append(" IN (");
            for (int i = 0; i < arrayOfParameterValue.length; i++) {
                if (i == 0) {
                    clause.append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                } else {
                    clause.append(",").append(AbcMssqlUtils.surroundBySingleQuotes(arrayOfParameterValue[i]));
                }
            }
            clause.append(")");
        }
        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的 IS NULL 条件部分
     *
     * @param parameterName
     * @return
     */
    private String buildIsNullParameter(String parameterName) {
        StringBuilder clause = new StringBuilder();
        clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                .append(" IS NULL");
        return clause.toString();
    }

    /**
     * 构建 WHERE 从句中的 IS NOT NULL 条件部分
     *
     * @param parameterName
     * @return
     */
    private String buildIsNotNullParameter(String parameterName) {
        StringBuilder clause = new StringBuilder();
        clause.append(AbcMssqlUtils.buildEscapedName(parameterName))
                .append(" IS NOT NULL");
        return clause.toString();
    }

    private String buildSortClause(AbcSort sort) {
        if (sort == null || CollectionUtils.isEmpty(sort.getOrders())) {
            return null;
        }

        StringBuilder clause = new StringBuilder();
        for (AbcOrder order : sort.getOrders()) {
            if (clause.length() > 0) {
                clause.append(",")
                        .append(AbcMssqlUtils.buildEscapedName(order.getProperty()))
                        .append(" ")
                        .append(order.getDirection());
            } else {
                clause.append(AbcMssqlUtils.buildEscapedName(order.getProperty()))
                        .append(" ")
                        .append(order.getDirection());
            }
        }

        return clause.toString();
    }

    private String buildPaginationClause(AbcPagination pagination) {
        if (pagination == null) {
            return null;
        }

        StringBuilder clause = new StringBuilder();
        clause.append("OFFSET " + pagination.getPage() * pagination.getSize())
                .append(" ROWS FETCH NEXT " + pagination.getSize())
                .append(" ROWS ONLY");

        return clause.toString();
    }
}
