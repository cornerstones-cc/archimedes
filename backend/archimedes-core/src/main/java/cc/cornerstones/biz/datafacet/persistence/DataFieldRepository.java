package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.DataFieldDo;
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
public interface DataFieldRepository extends PagingAndSortingRepository<DataFieldDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByName(String name);

    DataFieldDo findByUid(Long uid);

    List<DataFieldDo> findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataFieldDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<DataFieldDo> findAll(Specification<DataFieldDo> specification, Pageable pageable);

    List<DataFieldDo> findAll(Specification<DataFieldDo> specification, Sort sort);
}
