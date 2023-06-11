package cc.cornerstones.biz.datafacet.persistence;

import cc.cornerstones.biz.datafacet.entity.ExportExtendedTemplateDo;
import cc.cornerstones.biz.datafacet.share.constants.ExportExtendedTemplateVisibilityEnum;
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
public interface ExportExtendedTemplateRepository extends PagingAndSortingRepository<ExportExtendedTemplateDo, Long> {
    boolean existsByNameAndDataFacetUid(String name, Long dataFacetUid);

    ExportExtendedTemplateDo findByUid(Long uid);

    List<ExportExtendedTemplateDo> findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM ExportExtendedTemplateDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<ExportExtendedTemplateDo> findAll(Specification<ExportExtendedTemplateDo> specification, Pageable pageable);

    List<ExportExtendedTemplateDo> findAll(Specification<ExportExtendedTemplateDo> specification, Sort sort);

    boolean existsByNameAndDataFacetUidAndVisibility(
            String name,
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility);

    boolean existsByNameAndDataFacetUidAndVisibilityAndCreatedBy(
            String name,
            Long dataFacetUid,
            ExportExtendedTemplateVisibilityEnum visibility,
            Long createdBy);
}
