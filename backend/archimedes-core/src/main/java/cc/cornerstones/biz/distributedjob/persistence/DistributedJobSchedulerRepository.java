package cc.cornerstones.biz.distributedjob.persistence;

import cc.cornerstones.biz.distributedjob.entity.DistributedJobSchedulerDo;
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
public interface DistributedJobSchedulerRepository extends PagingAndSortingRepository<DistributedJobSchedulerDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByHostnameAndIpAddress(String hostname, String ipAddress);

    DistributedJobSchedulerDo findByUid(Long uid);

    Page<DistributedJobSchedulerDo> findAll(Specification<DistributedJobSchedulerDo> specification, Pageable pageable);

    List<DistributedJobSchedulerDo> findAll(Specification<DistributedJobSchedulerDo> specification, Sort sort);

    @Query("SELECT u FROM DistributedJobSchedulerDo u WHERE u.effective = 1")
    DistributedJobSchedulerDo findEffective();
}
