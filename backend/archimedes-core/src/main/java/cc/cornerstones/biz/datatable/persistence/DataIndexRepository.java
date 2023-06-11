package cc.cornerstones.biz.datatable.persistence;

import cc.cornerstones.biz.datatable.entity.DataIndexDo;
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
public interface DataIndexRepository extends PagingAndSortingRepository<DataIndexDo, Long> {
    boolean existsByUid(Long uid);

    DataIndexDo findByUid(Long uid);

    List<DataIndexDo> findByDataTableUid(Long dataTableUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataIndexDo u WHERE u.dataTableUid = ?1")
    void deleteByDataTableUid(Long dataTableUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataIndexDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    Page<DataIndexDo> findAll(Specification<DataIndexDo> specification, Pageable pageable);

    List<DataIndexDo> findAll(Specification<DataIndexDo> specification, Sort sort);
}
