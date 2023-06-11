package cc.cornerstones.biz.distributedjob.persistence;

import cc.cornerstones.biz.distributedjob.entity.DistributedJobExecutionDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DistributedJobExecutionRepository extends PagingAndSortingRepository<DistributedJobExecutionDo, Long> {
    boolean existsByUid(Long uid);

    DistributedJobExecutionDo findByUid(Long uid);

    Page<DistributedJobExecutionDo> findAll(Specification<DistributedJobExecutionDo> specification, Pageable pageable);

    Iterable<DistributedJobExecutionDo> findAll(Specification<DistributedJobExecutionDo> specification, Sort sort);
}
