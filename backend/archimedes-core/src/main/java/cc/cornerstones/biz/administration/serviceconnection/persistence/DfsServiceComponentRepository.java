package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.DfsServiceComponentDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DfsServiceComponentRepository extends PagingAndSortingRepository<DfsServiceComponentDo, Long> {
    boolean existsByUid(Long uid);

    DfsServiceComponentDo findByUid(Long uid);

    boolean existsByName(String name);

    DfsServiceComponentDo findByName(String name);

    Page<DfsServiceComponentDo> findAll(Specification<DfsServiceComponentDo> specification, Pageable pageable);

    List<DfsServiceComponentDo> findAll(Specification<DfsServiceComponentDo> specification, Sort sort);
}
