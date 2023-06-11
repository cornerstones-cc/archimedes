package cc.cornerstones.biz.datatable.persistence;

import cc.cornerstones.biz.datatable.entity.ContextPathMetadataRetrievalInstanceDo;
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
public interface ContextPathMetadataRetrievalInstanceRepository
        extends PagingAndSortingRepository<ContextPathMetadataRetrievalInstanceDo, Long> {
    boolean existsByUid(Long uid);

    ContextPathMetadataRetrievalInstanceDo findByUid(Long uid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM ContextPathMetadataRetrievalInstanceDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    Page<ContextPathMetadataRetrievalInstanceDo> findAll(Specification<ContextPathMetadataRetrievalInstanceDo> specification, Pageable pageable);

    List<ContextPathMetadataRetrievalInstanceDo> findAll(Specification<ContextPathMetadataRetrievalInstanceDo> specification, Sort sort);

}
