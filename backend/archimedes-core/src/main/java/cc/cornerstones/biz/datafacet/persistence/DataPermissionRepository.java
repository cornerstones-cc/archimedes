package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.DataPermissionDo;
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
public interface DataPermissionRepository extends PagingAndSortingRepository<DataPermissionDo, Long> {
    boolean existsByUid(Long uid);

    DataPermissionDo findByUid(Long uid);

    List<DataPermissionDo> findByDataFacetUid(Long dataFacetUid);

    List<DataPermissionDo> findByDataFacetUidAndEnabled(Long dataFacetUid, Boolean enabled);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataPermissionDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<DataPermissionDo> findAll(Specification<DataPermissionDo> specification, Pageable pageable);

    List<DataPermissionDo> findAll(Specification<DataPermissionDo> specification, Sort sort);
}
