package cc.cornerstones.biz.datatable.persistence;

import cc.cornerstones.biz.datatable.entity.DataColumnDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author bbottong
 */
public interface DataColumnRepository extends PagingAndSortingRepository<DataColumnDo, Long> {
    boolean existsByUid(Long uid);

    DataColumnDo findByUid(Long uid);

    List<DataColumnDo> findByDataTableUid(Long dataTableUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataColumnDo u WHERE u.dataTableUid = ?1")
    void deleteByDataTableUid(Long dataTableUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataColumnDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("UPDATE DataColumnDo u SET u.deleted = 1, u.lastModifiedBy = ?2, u.lastModifiedTimestamp = ?3 WHERE u" +
            ".dataSourceUid = ?1")
    void logicalDeleteByDataSourceUid(Long dataSourceUid, Long lastModifiedBy, LocalDateTime lastModifiedTimestamp);

    Page<DataColumnDo> findAll(Specification<DataColumnDo> specification, Pageable pageable);

    List<DataColumnDo> findAll(Specification<DataColumnDo> specification, Sort sort);
}
