package cc.cornerstones.arbutus.structuredlogging.service.inf;

import cc.cornerstones.arbutus.structuredlogging.dto.LogDto;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;

public interface LogService {

    LogDto getLog(String jobCategory, String jobUid) throws AbcUndefinedException;

    LogDto createLog(String jobCategory, String jobUid, String content) throws AbcUndefinedException;

    LogDto createLog(String jobCategory, Long jobUid, String content) throws AbcUndefinedException;
}
