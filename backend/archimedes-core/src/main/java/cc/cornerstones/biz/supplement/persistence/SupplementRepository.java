package cc.cornerstones.biz.supplement.persistence;

import cc.cornerstones.biz.supplement.entity.SupplementDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface SupplementRepository extends PagingAndSortingRepository<SupplementDo, Long> {
    boolean existsByName(String name);

    SupplementDo findByName(String name);

    Page<SupplementDo> findAll(Specification<SupplementDo> specification, Pageable pageable);

    List<SupplementDo> findAll(Specification<SupplementDo> specification, Sort sort);
}
