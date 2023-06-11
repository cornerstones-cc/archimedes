package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.ListingExtendedDo;
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
public interface ListingExtendedRepository extends PagingAndSortingRepository<ListingExtendedDo, Long> {
    ListingExtendedDo findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM ListingExtendedDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<ListingExtendedDo> findAll(Specification<ListingExtendedDo> specification, Pageable pageable);

    List<ListingExtendedDo> findAll(Specification<ListingExtendedDo> specification, Sort sort);
}
