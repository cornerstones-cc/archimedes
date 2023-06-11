package cc.cornerstones.biz.datasource.persistence;


import cc.cornerstones.biz.datasource.entity.DataSourceDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DataSourceRepository extends PagingAndSortingRepository<DataSourceDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByName(String name);

    boolean existsByConnectionProfileHashedString(String connectionProfileHashedString);

    DataSourceDo findByUid(Long uid);

    DataSourceDo findByName(String name);

    @Query("SELECT COUNT(u) FROM DataSourceDo u WHERE u.databaseServerUid = ?1")
    Integer countByDatabaseServerUid(Long databaseServerUid);

    Page<DataSourceDo> findAll(Specification<DataSourceDo> specification, Pageable pageable);

    List<DataSourceDo> findAll(Specification<DataSourceDo> specification, Sort sort);
}
