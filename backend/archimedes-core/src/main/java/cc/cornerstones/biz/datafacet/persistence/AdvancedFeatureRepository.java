package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.AdvancedFeatureDo;
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
public interface AdvancedFeatureRepository extends PagingAndSortingRepository<AdvancedFeatureDo, Long> {
    AdvancedFeatureDo findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM AdvancedFeatureDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<AdvancedFeatureDo> findAll(Specification<AdvancedFeatureDo> specification, Pageable pageable);

    List<AdvancedFeatureDo> findAll(Specification<AdvancedFeatureDo> specification, Sort sort);
}
