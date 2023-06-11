package cc.cornerstones.biz.datawidget.persistence;

import cc.cornerstones.biz.datawidget.entity.DataWidgetDo;
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
public interface DataWidgetRepository extends PagingAndSortingRepository<DataWidgetDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByNameAndDataFacetUid(String name, Long dataFacetUid);

    DataWidgetDo findByUid(Long uid);

    List<DataWidgetDo> findByDataFacetUid(Long dataFacetUid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataWidgetDo u WHERE u.dataFacetUid = ?1")
    void deleteByDataFacetUid(Long dataFacetUid);

    Page<DataWidgetDo> findAll(Specification<DataWidgetDo> specification, Pageable pageable);

    List<DataWidgetDo> findAll(Specification<DataWidgetDo> specification, Sort sort);
}
