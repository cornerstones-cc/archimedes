package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceAgentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface UserSynchronizationServiceAgentRepository extends PagingAndSortingRepository<UserSynchronizationServiceAgentDo, Long> {
    boolean existsByUid(Long uid);

    UserSynchronizationServiceAgentDo findByUid(Long uid);

    List<UserSynchronizationServiceAgentDo> findByServiceComponentUid(Long serviceComponentUid);

    boolean existsByName(String name);

    UserSynchronizationServiceAgentDo findByName(String name);

    Page<UserSynchronizationServiceAgentDo> findAll(Specification<UserSynchronizationServiceAgentDo> specification, Pageable pageable);

    List<UserSynchronizationServiceAgentDo> findAll(Specification<UserSynchronizationServiceAgentDo> specification, Sort sort);
}
