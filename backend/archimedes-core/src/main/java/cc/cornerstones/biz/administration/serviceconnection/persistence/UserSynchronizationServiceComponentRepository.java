package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationServiceComponentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface UserSynchronizationServiceComponentRepository extends PagingAndSortingRepository<UserSynchronizationServiceComponentDo, Long> {
    boolean existsByUid(Long uid);

    UserSynchronizationServiceComponentDo findByUid(Long uid);

    boolean existsByName(String name);

    UserSynchronizationServiceComponentDo findByName(String name);

    Page<UserSynchronizationServiceComponentDo> findAll(Specification<UserSynchronizationServiceComponentDo> specification, Pageable pageable);

    List<UserSynchronizationServiceComponentDo> findAll(Specification<UserSynchronizationServiceComponentDo> specification, Sort sort);
}
