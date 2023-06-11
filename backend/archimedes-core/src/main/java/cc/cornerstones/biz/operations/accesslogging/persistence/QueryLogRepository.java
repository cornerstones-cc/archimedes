package cc.cornerstones.biz.operations.accesslogging.persistence;

import cc.cornerstones.biz.operations.accesslogging.entity.QueryLogDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * @author bbottong
 */
public interface QueryLogRepository extends PagingAndSortingRepository<QueryLogDo, Long> {
    QueryLogDo findByTrackingSerialNumber(String trackingSerialNumber);

    Page<QueryLogDo> findAll(Specification<QueryLogDo> specification, Pageable pageable);
}
