package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserSchemaExtendedPropertyDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface UserSchemaExtendedPropertyRepository extends PagingAndSortingRepository<UserSchemaExtendedPropertyDo, Long> {
    boolean existsByUid(Long uid);

    UserSchemaExtendedPropertyDo findByUid(Long uid);

    boolean existsByName(String name);

    UserSchemaExtendedPropertyDo findByName(String name);

    Page<UserSchemaExtendedPropertyDo> findAll(Specification<UserSchemaExtendedPropertyDo> specification, Pageable pageable);

    List<UserSchemaExtendedPropertyDo> findAll(Specification<UserSchemaExtendedPropertyDo> specification, Sort sort);
}
