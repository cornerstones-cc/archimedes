package cc.cornerstones.arbutus.structuredlogging.persistence;

import cc.cornerstones.arbutus.structuredlogging.entity.LogDo;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface LogRepository extends PagingAndSortingRepository<LogDo, Long> {

    /**
     * 按job category和job uid查找
     *
     * @param jobCategory
     * @param jobUid
     * @return
     */
    LogDo findByJobCategoryAndJobUid(String jobCategory, String jobUid);
}
