package cc.cornerstones.biz.distributedtask.persistence;

import cc.cornerstones.biz.distributedtask.entity.DistributedTaskDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface DistributedTaskRepository extends PagingAndSortingRepository<DistributedTaskDo, Long> {
    boolean existsByUid(Long uid);

    DistributedTaskDo findByUid(Long uid);

    Page<DistributedTaskDo> findAll(Specification<DistributedTaskDo> specification, Pageable pageable);

    Iterable<DistributedTaskDo> findAll(Specification<DistributedTaskDo> specification, Sort sort);
}
