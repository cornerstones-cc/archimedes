package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceAgentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DfsServiceAgentRepository extends PagingAndSortingRepository<DfsServiceAgentDo, Long> {
    boolean existsByUid(Long uid);

    DfsServiceAgentDo findByUid(Long uid);

    List<DfsServiceAgentDo> findByServiceComponentUid(Long serviceComponentUid);

    List<DfsServiceAgentDo> findByEnabledAndPreferred(Boolean enabled, Boolean preferred);

    boolean existsByName(String name);

    DfsServiceAgentDo findByName(String name);

    Page<DfsServiceAgentDo> findAll(Specification<DfsServiceAgentDo> specification, Pageable pageable);

    List<DfsServiceAgentDo> findAll(Specification<DfsServiceAgentDo> specification, Sort sort);
}
