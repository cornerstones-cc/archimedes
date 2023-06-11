package cc.cornerstones.biz.operations.statisticalanalysis.service.impl;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.AbcTuple3;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.almond.utils.AbcStringUtils;
import cc.cornerstones.biz.administration.usermanagement.persistence.UserBasicRepository;
import cc.cornerstones.biz.app.persistence.AppRepository;
import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import cc.cornerstones.biz.datafacet.persistence.DataFacetRepository;
import cc.cornerstones.biz.export.persistence.ExportTaskRepository;
import cc.cornerstones.biz.operations.accesslogging.persistence.QueryLogRepository;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisDetailsDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisOverallDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisRankingDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisTrendingDto;
import cc.cornerstones.biz.operations.statisticalanalysis.service.inf.StatisticalAnalysisService;
import cc.cornerstones.biz.operations.statisticalanalysis.share.types.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatisticalAnalysisServiceImpl implements StatisticalAnalysisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticalAnalysisServiceImpl.class);

    @Autowired
    private AppRepository appRepository;

    @Autowired
    private DataFacetRepository dataFacetRepository;

    @Autowired
    private UserBasicRepository userBasicRepository;

    @Autowired
    private QueryLogRepository queryLogRepository;

    @Autowired
    private ExportTaskRepository exportTaskRepository;

    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private DateTimeFormatter monthDayFormatter = DateTimeFormatter.ofPattern("MM/dd");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public StatisticalAnalysisOverallDto getOverall(
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StatisticalAnalysisOverallDto result = new StatisticalAnalysisOverallDto();

        Long totalNumberOfApps = this.appRepository.count();
        Long totalNumberOfDataFacets = this.dataFacetRepository.count();
        Long totalNumberOfUsers = this.userBasicRepository.count();
        Long totalNumberOfQueries = this.queryLogRepository.count();
        Long totalNumberOfExports = this.exportTaskRepository.count();

        result.setTotalNumberOfApps(totalNumberOfApps);
        result.setTotalNumberOfDataFacets(totalNumberOfDataFacets);
        result.setTotalNumberOfUsers(totalNumberOfUsers);
        result.setTotalNumberOfQueries(totalNumberOfQueries);
        result.setTotalNumberOfExports(totalNumberOfExports);
        return result;
    }

    @Override
    public StatisticalAnalysisTrendingDto getTrending(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StatisticalAnalysisTrendingDto result = new StatisticalAnalysisTrendingDto();
        result.setQueries(getTrendingOfQueries(beginDateAsString, endDateAsString, operatingUserProfile));
        result.setExports(getTrendingOfExports(beginDateAsString, endDateAsString, operatingUserProfile));
        return result;
    }

    private List<Cell> getTrendingOfQueries(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT created_date, count(*) AS subtotal FROM f5_query_log")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" GROUP BY created_date")
                .append(" ORDER BY created_date ASC");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        List<Cell> result = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object createdDateAsObject = row.get("created_date");
            if (createdDateAsObject == null) {
                continue;
            }

            Cell cell = new Cell();

            String createdDateAsString = null;
            if (createdDateAsObject instanceof LocalDate) {
                LocalDate createdDateAsLocalDate = (LocalDate) createdDateAsObject;
                createdDateAsString = createdDateAsLocalDate.format(monthDayFormatter);
            } else {
                LOGGER.warn("unexpected type of created_date {}", createdDateAsObject);
                createdDateAsString = String.valueOf(createdDateAsObject);
            }

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            cell.setName(createdDateAsString);
            cell.setValue(valueAsLong);

            result.add(cell);
        }

        return result;
    }

    private List<Cell> getTrendingOfExports(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT created_date, count(*) AS subtotal FROM t5_export_task")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" GROUP BY created_date")
                .append(" ORDER BY created_date ASC");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        List<Cell> result = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object createdDateAsObject = row.get("created_date");
            if (createdDateAsObject == null) {
                continue;
            }

            Cell cell = new Cell();

            String createdDateAsString = null;
            if (createdDateAsObject instanceof LocalDate) {
                LocalDate createdDateAsLocalDate = (LocalDate) createdDateAsObject;
                createdDateAsString = createdDateAsLocalDate.format(monthDayFormatter);
            } else {
                LOGGER.warn("unexpected type of created_date {}", createdDateAsObject);
                createdDateAsString = String.valueOf(createdDateAsObject);
            }

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            cell.setName(createdDateAsString);
            cell.setValue(valueAsLong);

            result.add(cell);
        }

        return result;
    }

    @Override
    public StatisticalAnalysisRankingDto getRanking(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StatisticalAnalysisRankingDto result = new StatisticalAnalysisRankingDto();
        result.setQueries(getRankingOfQueries(beginDateAsString, endDateAsString, operatingUserProfile));
        result.setExports(getRankingOfExports(beginDateAsString, endDateAsString, operatingUserProfile));
        return result;
    }

    private List<Cell> getRankingOfQueries(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT data_facet_uid, count(*) AS subtotal FROM f5_query_log")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" GROUP BY data_facet_uid")
                .append(" ORDER BY subtotal DESC")
                .append(" LIMIT 0, 30");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        List<Long> dataFacetUidList = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Long dataFacetUid = (Long) dataFacetUidAsObject;
            dataFacetUidList.add(dataFacetUid);
        }

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByUidIn(dataFacetUidList);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }
        Map<Long, DataFacetDo> dataFacetDoMap = new HashMap<>();
        dataFacetDoList.forEach(dataFacetDo -> {
            dataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
        });

        List<Cell> result = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Cell cell = new Cell();

            Long dataFacetUid = (Long) dataFacetUidAsObject;

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            if (dataFacetDoMap.containsKey(dataFacetUid)) {
                cell.setName(dataFacetDoMap.get(dataFacetUid).getName());
                cell.setValue(valueAsLong);

                result.add(cell);
            }
        }

        return result;
    }

    private List<Cell> getRankingOfExports(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT data_facet_uid, count(*) AS subtotal FROM t5_export_task")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" GROUP BY data_facet_uid")
                .append(" ORDER BY subtotal DESC")
                .append(" LIMIT 0, 30");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        // prepare data facet
        List<Long> dataFacetUidList = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Long dataFacetUid = (Long) dataFacetUidAsObject;
            dataFacetUidList.add(dataFacetUid);
        }

        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByUidIn(dataFacetUidList);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }
        Map<Long, DataFacetDo> dataFacetDoMap = new HashMap<>();
        dataFacetDoList.forEach(dataFacetDo -> {
            dataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
        });

        //
        List<Cell> result = new LinkedList<>();
        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Cell cell = new Cell();

            Long dataFacetUid = (Long) dataFacetUidAsObject;

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            if (dataFacetDoMap.containsKey(dataFacetUid)) {
                cell.setName(dataFacetDoMap.get(dataFacetUid).getName());
                cell.setValue(valueAsLong);

                result.add(cell);
            }
        }

        return result;
    }


    @Override
    public StatisticalAnalysisDetailsDto getDetailsOfQuery(
            String beginDateAsString,
            String endDateAsString,
            List<Long> dataFacetUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (CollectionUtils.isEmpty(dataFacetUidList)) {
            return null;
        }

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT data_facet_uid, created_date, count(*) AS subtotal FROM f5_query_log")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" AND").append(" data_facet_uid IN").append(" (" + AbcStringUtils.toString(dataFacetUidList, ",") + ")")
                .append(" GROUP BY data_facet_uid, created_date")
                .append(" ORDER BY data_facet_uid ASC, created_date ASC");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        //
        // prepare data facet
        //
        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByUidIn(dataFacetUidList);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }
        Map<Long, DataFacetDo> dataFacetDoMap = new HashMap<>();
        dataFacetDoList.forEach(dataFacetDo -> {
            dataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
        });

        //
        // Step 3, post-processing
        //

        StatisticalAnalysisDetailsDto result = new StatisticalAnalysisDetailsDto();
        result.setUnits(new LinkedList<>());

        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Object createdDateAsObject = row.get("created_date");
            if (createdDateAsObject == null) {
                continue;
            }

            Long dataFacetUid = (Long) dataFacetUidAsObject;
            if (!dataFacetDoMap.containsKey(dataFacetUid)) {
                continue;
            }

            String createdDateAsString = null;
            if (createdDateAsObject instanceof LocalDate) {
                LocalDate createdDateAsLocalDate = (LocalDate) createdDateAsObject;
                createdDateAsString = createdDateAsLocalDate.format(monthDayFormatter);
            } else {
                LOGGER.warn("unexpected type of created_date {}", createdDateAsObject);
                createdDateAsString = String.valueOf(createdDateAsObject);
            }

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            String dataFacetName = dataFacetDoMap.get(dataFacetUid).getName();
            result.getUnits().add(new AbcTuple3<>(dataFacetName, createdDateAsString, valueAsLong));
        }

        return result;
    }

    @Override
    public StatisticalAnalysisDetailsDto getDetailsOfExport(
            String beginDateAsString,
            String endDateAsString,
            List<Long> dataFacetUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException {
        if (CollectionUtils.isEmpty(dataFacetUidList)) {
            return null;
        }

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT data_facet_uid, created_date, count(*) AS subtotal FROM t5_export_task")
                .append(" WHERE is_deleted = 0")
                .append(" AND").append(" created_date BETWEEN").append(" '" + beginDateAsString + "'").append(" AND").append(" '" + endDateAsString + "'")
                .append(" AND").append(" data_facet_uid IN").append(" (" + AbcStringUtils.toString(dataFacetUidList, ",") + ")")
                .append(" GROUP BY data_facet_uid, created_date")
                .append(" ORDER BY data_facet_uid ASC, created_date ASC");

        String sqlAsString = sql.toString();
        List<Map<String, Object>> queryResult = this.jdbcTemplate.queryForList(sqlAsString);
        if (CollectionUtils.isEmpty(queryResult)) {
            return null;
        }

        //
        // prepare data facet
        //
        List<DataFacetDo> dataFacetDoList = this.dataFacetRepository.findByUidIn(dataFacetUidList);
        if (CollectionUtils.isEmpty(dataFacetDoList)) {
            return null;
        }
        Map<Long, DataFacetDo> dataFacetDoMap = new HashMap<>();
        dataFacetDoList.forEach(dataFacetDo -> {
            dataFacetDoMap.put(dataFacetDo.getUid(), dataFacetDo);
        });

        //
        // Step 3, post-processing
        //

        StatisticalAnalysisDetailsDto result = new StatisticalAnalysisDetailsDto();
        result.setUnits(new LinkedList<>());

        for (Map<String, Object> row : queryResult) {
            Object dataFacetUidAsObject = row.get("data_facet_uid");
            if (dataFacetUidAsObject == null) {
                continue;
            }

            Object createdDateAsObject = row.get("created_date");
            if (createdDateAsObject == null) {
                continue;
            }

            Long dataFacetUid = (Long) dataFacetUidAsObject;
            if (!dataFacetDoMap.containsKey(dataFacetUid)) {
                continue;
            }

            String createdDateAsString = null;
            if (createdDateAsObject instanceof LocalDate) {
                LocalDate createdDateAsLocalDate = (LocalDate) createdDateAsObject;
                createdDateAsString = createdDateAsLocalDate.format(monthDayFormatter);
            } else {
                LOGGER.warn("unexpected type of created_date {}", createdDateAsObject);
                createdDateAsString = String.valueOf(createdDateAsObject);
            }

            Long valueAsLong = null;
            Object valueAsObject = row.get("subtotal");
            if (valueAsObject instanceof Integer) {
                valueAsLong = ((Integer) valueAsObject).longValue();
            } else if (valueAsObject instanceof Long) {
                valueAsLong = (Long) valueAsObject;
            }

            String dataFacetName = dataFacetDoMap.get(dataFacetUid).getName();
            result.getUnits().add(new AbcTuple3<>(dataFacetName, createdDateAsString, valueAsLong));
        }

        return result;
    }
}
