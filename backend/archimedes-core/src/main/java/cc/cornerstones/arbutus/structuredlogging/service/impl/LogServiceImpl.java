package cc.cornerstones.arbutus.structuredlogging.service.impl;

import cc.cornerstones.arbutus.structuredlogging.entity.LogDo;
import cc.cornerstones.arbutus.structuredlogging.dto.LogDto;
import cc.cornerstones.arbutus.structuredlogging.persistence.LogRepository;
import cc.cornerstones.arbutus.structuredlogging.service.inf.LogService;
import cc.cornerstones.almond.constants.InfrastructureConstants;
import cc.cornerstones.almond.exceptions.AbcResourceNotFoundException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;
import cc.cornerstones.almond.types.BaseDo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LogServiceImpl implements LogService {
    @Autowired
    private LogRepository logRepository;

    @Override
    public LogDto getLog(
            String jobCategory,
            String jobUid) throws AbcUndefinedException {
        LogDo logDo = this.logRepository.findByJobCategoryAndJobUid(jobCategory, jobUid);
        if (logDo == null) {
            throw new AbcResourceNotFoundException(String.format("%s::job_category=%s, job_uid=%s", LogDo.RESOURCE_SYMBOL,
                    jobCategory, jobUid));
        }

        LogDto logDto = new LogDto();
        BeanUtils.copyProperties(logDo, logDto);
        return logDto;
    }

    @Override
    public LogDto createLog(
            String jobCategory,
            Long jobUid,
            String content) throws AbcUndefinedException {
        return createLog(jobCategory, String.valueOf(jobUid), content);
    }

    @Override
    public LogDto createLog(
            String jobCategory,
            String jobUid,
            String content) throws AbcUndefinedException {
        LogDo logDo = new LogDo();
        logDo.setJobCategory(jobCategory);
        logDo.setJobUid(jobUid);
        logDo.setContent(content);
        BaseDo.create(logDo, InfrastructureConstants.ROOT_USER_UID, LocalDateTime.now());
        this.logRepository.save(logDo);

        LogDto logDto = new LogDto();
        BeanUtils.copyProperties(logDo, logDto);
        return logDto;
    }
}
