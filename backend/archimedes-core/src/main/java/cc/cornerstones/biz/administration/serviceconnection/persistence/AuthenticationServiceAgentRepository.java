package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceAgentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface AuthenticationServiceAgentRepository extends PagingAndSortingRepository<AuthenticationServiceAgentDo, Long> {
    boolean existsByUid(Long uid);

    AuthenticationServiceAgentDo findByUid(Long uid);

    List<AuthenticationServiceAgentDo> findByServiceComponentUid(Long serviceComponentUid);

    boolean existsByName(String name);

    AuthenticationServiceAgentDo findByName(String name);

    Page<AuthenticationServiceAgentDo> findAll(Specification<AuthenticationServiceAgentDo> specification, Pageable pageable);

    List<AuthenticationServiceAgentDo> findAll(Specification<AuthenticationServiceAgentDo> specification, Sort sort);
}
