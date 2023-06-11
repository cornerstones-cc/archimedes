package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.FilteringDataFieldDo;
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
public interface FilteringDataFieldRepository extends PagingAndSortingRepository<FilteringDataFieldDo, Long> {
    List<FilteringDataFieldDo> findByDataFacetUid(Long dataFacetUid, Sort sort);

    List<FilteringDataFieldDo> findByDataFacetUidAndFieldNameIn(Long dataFacetUid, List<String> fieldNameList);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM FilteringDataFieldDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<FilteringDataFieldDo> findAll(Specification<FilteringDataFieldDo> specification, Pageable pageable);

    List<FilteringDataFieldDo> findAll(Specification<FilteringDataFieldDo> specification, Sort sort);
}
