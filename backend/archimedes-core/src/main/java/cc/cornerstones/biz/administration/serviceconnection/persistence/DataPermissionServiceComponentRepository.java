package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.DataPermissionServiceComponentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DataPermissionServiceComponentRepository extends PagingAndSortingRepository<DataPermissionServiceComponentDo, Long> {
    boolean existsByUid(Long uid);

    DataPermissionServiceComponentDo findByUid(Long uid);

    boolean existsByName(String name);

    DataPermissionServiceComponentDo findByName(String name);

    Page<DataPermissionServiceComponentDo> findAll(Specification<DataPermissionServiceComponentDo> specification, Pageable pageable);

    List<DataPermissionServiceComponentDo> findAll(Specification<DataPermissionServiceComponentDo> specification, Sort sort);
}
