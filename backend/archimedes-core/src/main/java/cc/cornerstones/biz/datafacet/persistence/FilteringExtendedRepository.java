package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.FilteringExtendedDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author bbottong
 */
public interface FilteringExtendedRepository extends PagingAndSortingRepository<FilteringExtendedDo, Long> {
    FilteringExtendedDo findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM FilteringExtendedDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<FilteringExtendedDo> findAll(Specification<FilteringExtendedDo> specification, Pageable pageable);

    List<FilteringExtendedDo> findAll(Specification<FilteringExtendedDo> specification, Sort sort);
}
