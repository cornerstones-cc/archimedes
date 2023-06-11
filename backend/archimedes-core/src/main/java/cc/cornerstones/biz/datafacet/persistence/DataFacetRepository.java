package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.DataFacetDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author bbottong
 */
public interface DataFacetRepository extends PagingAndSortingRepository<DataFacetDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByName(String name);

    DataFacetDo findByUid(Long uid);

    List<DataFacetDo> findByDataSourceUid(Long dataSourceUid);


    List<DataFacetDo> findByDataTableUid(Long dataTableUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataFacetDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("UPDATE DataFacetDo u SET u.deleted = 1, u.lastModifiedBy = ?2, u.lastModifiedTimestamp = ?3 WHERE u" +
            ".dataSourceUid = ?1")
    void logicalDeleteByDataSourceUid(Long dataSourceUid, Long lastModifiedBy, LocalDateTime lastModifiedTimestamp);

    Page<DataFacetDo> findAll(Specification<DataFacetDo> specification, Pageable pageable);

    List<DataFacetDo> findAll(Specification<DataFacetDo> specification, Sort sort);


    List<DataFacetDo> findByUidIn(List<Long> uidList);
}
