package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.SortingDataFieldDo;
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
public interface SortingDataFieldRepository extends PagingAndSortingRepository<SortingDataFieldDo, Long> {
    List<SortingDataFieldDo> findByDataFacetUid(Long dataFacetUid, Sort sort);

    List<SortingDataFieldDo> findByDataFacetUidAndFieldNameIn(Long dataFacetUid, List<String> fieldNameList);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM SortingDataFieldDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<SortingDataFieldDo> findAll(Specification<SortingDataFieldDo> specification, Pageable pageable);

    List<SortingDataFieldDo> findAll(Specification<SortingDataFieldDo> specification, Sort sort);
}
