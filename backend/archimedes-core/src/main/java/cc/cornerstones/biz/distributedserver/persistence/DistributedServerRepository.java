package cc.cornerstones.biz.distributedserver.persistence;

import cc.cornerstones.biz.distributedserver.entity.DistributedServerDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DistributedServerRepository extends PagingAndSortingRepository<DistributedServerDo, Long> {
    boolean existsByHostnameAndIpAddress(String hostname, String ipAddress);

    DistributedServerDo findByHostnameAndIpAddress(String hostname, String ipAddress);

    Page<DistributedServerDo> findAll(Specification<DistributedServerDo> specification, Pageable pageable);

    List<DistributedServerDo> findAll(Specification<DistributedServerDo> specification, Sort sort);
}
