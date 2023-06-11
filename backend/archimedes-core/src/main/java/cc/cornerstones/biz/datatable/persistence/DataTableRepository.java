package cc.cornerstones.biz.datatable.persistence;

import cc.cornerstones.biz.datasource.share.constants.DataTableTypeEnum;
import cc.cornerstones.biz.datatable.entity.DataTableDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public interface DataTableRepository extends PagingAndSortingRepository<DataTableDo, Long> {
    boolean existsByUid(Long uid);

    DataTableDo findByUid(Long uid);

    List<DataTableDo> findByUidIn(List<Long> uidList);

    List<DataTableDo> findByDataSourceUid(Long dataSourceUid);

    List<DataTableDo> findByDataSourceUidAndType(Long dataSourceUid, DataTableTypeEnum type);

    List<DataTableDo> findByDataSourceUidAndTypeIn(Long dataSourceUid, List<DataTableTypeEnum> typeList);

    List<DataTableDo> findByDataSourceUidAndName(Long dataSourceUid, String name);

    @Query("SELECT COUNT(u) FROM DataTableDo u WHERE u.dataSourceUid = ?1 AND u.name = ?2 AND u.contextPath IS NULL")
    Integer countByDataSourceUidAndNameWithNullContextPath(Long dataSourceUid, String name);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataTableDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("UPDATE DataTableDo u SET u.deleted = 1, u.lastModifiedBy = ?2, u.lastModifiedTimestamp = ?3 WHERE u" +
            ".dataSourceUid = ?1")
    void logicalDeleteByDataSourceUid(Long dataSourceUid, Long lastModifiedBy, LocalDateTime lastModifiedTimestamp);


    Page<DataTableDo> findAll(Specification<DataTableDo> specification, Pageable pageable);
}
