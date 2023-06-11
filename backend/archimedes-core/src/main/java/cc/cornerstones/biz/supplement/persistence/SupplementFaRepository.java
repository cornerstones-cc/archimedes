package cc.cornerstones.biz.supplement.persistence;

import cc.cornerstones.biz.supplement.entity.SupplementFaDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface SupplementFaRepository extends PagingAndSortingRepository<SupplementFaDo, Long> {

    Page<SupplementFaDo> findAll(Specification<SupplementFaDo> specification, Pageable pageable);
}
