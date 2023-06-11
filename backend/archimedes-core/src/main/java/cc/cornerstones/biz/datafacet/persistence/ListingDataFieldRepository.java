package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.ListingDataFieldDo;
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
public interface ListingDataFieldRepository extends PagingAndSortingRepository<ListingDataFieldDo, Long> {
    List<ListingDataFieldDo> findByDataFacetUid(Long dataFacetUid, Sort sort);

    List<ListingDataFieldDo> findByDataFacetUidAndFieldNameIn(Long dataFacetUid, List<String> fieldNameList);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM ListingDataFieldDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<ListingDataFieldDo> findAll(Specification<ListingDataFieldDo> specification, Pageable pageable);

    List<ListingDataFieldDo> findAll(Specification<ListingDataFieldDo> specification, Sort sort);
}
