package cc.cornerstones.biz.datasource.persistence;

import cc.cornerstones.biz.datasource.entity.DataSourceMetadataRetrievalInstanceDo;
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
public interface DataSourceMetadataRetrievalInstanceRepository
        extends PagingAndSortingRepository<DataSourceMetadataRetrievalInstanceDo, Long> {
    boolean existsByUid(Long uid);

    DataSourceMetadataRetrievalInstanceDo findByUid(Long uid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataSourceMetadataRetrievalInstanceDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    Page<DataSourceMetadataRetrievalInstanceDo> findAll(Specification<DataSourceMetadataRetrievalInstanceDo> specification, Pageable pageable);

    List<DataSourceMetadataRetrievalInstanceDo> findAll(Specification<DataSourceMetadataRetrievalInstanceDo> specification, Sort sort);

}
