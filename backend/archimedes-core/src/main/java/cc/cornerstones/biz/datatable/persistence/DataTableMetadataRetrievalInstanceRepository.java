package cc.cornerstones.biz.datatable.persistence;

import cc.cornerstones.biz.datatable.entity.DataTableMetadataRetrievalInstanceDo;
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
public interface DataTableMetadataRetrievalInstanceRepository
        extends PagingAndSortingRepository<DataTableMetadataRetrievalInstanceDo, Long> {
    boolean existsByUid(Long uid);

    DataTableMetadataRetrievalInstanceDo findByUid(Long uid);

    @Transactional(rollbackFor = Exception.class)
    @Modifying
    @Query("DELETE FROM DataTableMetadataRetrievalInstanceDo u WHERE u.dataSourceUid = ?1")
    void deleteByDataSourceUid(Long dataSourceUid);

    Page<DataTableMetadataRetrievalInstanceDo> findAll(Specification<DataTableMetadataRetrievalInstanceDo> specification, Pageable pageable);

    List<DataTableMetadataRetrievalInstanceDo> findAll(Specification<DataTableMetadataRetrievalInstanceDo> specification, Sort sort);

}
