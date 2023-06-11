package cc.cornerstones.biz.distributedjob.persistence;

import cc.cornerstones.biz.distributedjob.entity.DistributedJobDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DistributedJobRepository extends PagingAndSortingRepository<DistributedJobDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByHashedString(String hashedString);

    DistributedJobDo findByUid(Long uid);

    Page<DistributedJobDo> findAll(Specification<DistributedJobDo> specification, Pageable pageable);

    Iterable<DistributedJobDo> findAll(Specification<DistributedJobDo> specification, Sort sort);
}
