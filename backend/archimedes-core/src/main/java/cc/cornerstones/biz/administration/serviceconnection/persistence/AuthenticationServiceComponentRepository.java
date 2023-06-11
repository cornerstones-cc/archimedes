package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.AuthenticationServiceComponentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface AuthenticationServiceComponentRepository extends PagingAndSortingRepository<AuthenticationServiceComponentDo, Long> {
    boolean existsByUid(Long uid);

    AuthenticationServiceComponentDo findByUid(Long uid);

    boolean existsByName(String name);

    AuthenticationServiceComponentDo findByName(String name);

    Page<AuthenticationServiceComponentDo> findAll(Specification<AuthenticationServiceComponentDo> specification, Pageable pageable);

    List<AuthenticationServiceComponentDo> findAll(Specification<AuthenticationServiceComponentDo> specification, Sort sort);
}
