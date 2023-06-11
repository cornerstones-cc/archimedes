package cc.cornerstones.biz.operations.statisticalanalysis.service.inf;

import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.TreeNode;
import cc.cornerstones.almond.types.UserProfile;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisDetailsDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisOverallDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisRankingDto;
import cc.cornerstones.biz.operations.statisticalanalysis.dto.StatisticalAnalysisTrendingDto;

import java.util.List;

public interface StatisticalAnalysisService {
    StatisticalAnalysisOverallDto getOverall(
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    StatisticalAnalysisTrendingDto getTrending(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    StatisticalAnalysisRankingDto getRanking(
            String beginDateAsString,
            String endDateAsString,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    StatisticalAnalysisDetailsDto getDetailsOfQuery(
            String beginDateAsString,
            String endDateAsString,
            List<Long> dataFacetUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;

    StatisticalAnalysisDetailsDto getDetailsOfExport(
            String beginDateAsString,
            String endDateAsString,
            List<Long> dataFacetUidList,
            UserProfile operatingUserProfile) throws AbcUndefinedException;
}
