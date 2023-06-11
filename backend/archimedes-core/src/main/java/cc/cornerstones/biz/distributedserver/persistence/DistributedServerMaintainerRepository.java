package cc.cornerstones.biz.distributedserver.persistence;

import cc.cornerstones.biz.distributedserver.entity.DistributedServerMaintainerDo;
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
public interface DistributedServerMaintainerRepository extends PagingAndSortingRepository<DistributedServerMaintainerDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByHostnameAndIpAddress(String hostname, String ipAddress);

    DistributedServerMaintainerDo findByUid(Long uid);

    Page<DistributedServerMaintainerDo> findAll(Specification<DistributedServerMaintainerDo> specification, Pageable pageable);

    List<DistributedServerMaintainerDo> findAll(Specification<DistributedServerMaintainerDo> specification, Sort sort);

    @Query("SELECT u FROM DistributedServerMaintainerDo u WHERE u.effective = 1")
    DistributedServerMaintainerDo findEffective();
}
