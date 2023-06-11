package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceAgentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DataPermissionServiceAgentRepository extends PagingAndSortingRepository<DataPermissionServiceAgentDo, Long> {
    boolean existsByUid(Long uid);

    DataPermissionServiceAgentDo findByUid(Long uid);

    List<DataPermissionServiceAgentDo> findByServiceComponentUid(Long serviceComponentUid);

    boolean existsByName(String name);

    DataPermissionServiceAgentDo findByName(String name);

    Page<DataPermissionServiceAgentDo> findAll(Specification<DataPermissionServiceAgentDo> specification, Pageable pageable);

    List<DataPermissionServiceAgentDo> findAll(Specification<DataPermissionServiceAgentDo> specification, Sort sort);
}
