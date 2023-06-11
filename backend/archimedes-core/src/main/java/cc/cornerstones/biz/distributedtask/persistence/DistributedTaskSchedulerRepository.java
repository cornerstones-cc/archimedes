package cc.cornerstones.biz.distributedtask.persistence;

import cc.cornerstones.biz.distributedtask.entity.DistributedTaskSchedulerDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DistributedTaskSchedulerRepository extends PagingAndSortingRepository<DistributedTaskSchedulerDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByHostnameAndIpAddress(String hostname, String ipAddress);

    DistributedTaskSchedulerDo findByUid(Long uid);

    Page<DistributedTaskSchedulerDo> findAll(Specification<DistributedTaskSchedulerDo> specification, Pageable pageable);

    List<DistributedTaskSchedulerDo> findAll(Specification<DistributedTaskSchedulerDo> specification, Sort sort);

    @Query("SELECT u FROM DistributedTaskSchedulerDo u WHERE u.effective = 1")
    DistributedTaskSchedulerDo findEffective();
}
