package cc.cornerstones.biz.datasource.persistence;

import cc.cornerstones.biz.datasource.entity.DatabaseServerDo;
import cc.cornerstones.biz.datasource.share.constants.DatabaseServerTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface DatabaseServerRepository extends PagingAndSortingRepository<DatabaseServerDo, Long> {
    boolean existsByUid(Long uid);

    DatabaseServerDo findByUid(Long uid);

    DatabaseServerDo findByTypeAndHashedHostProfile(DatabaseServerTypeEnum type, String hashedHostProfile);

    Page<DatabaseServerDo> findAll(Specification<DatabaseServerDo> specification, Pageable pageable);

    List<DatabaseServerDo> findAll(Specification<DatabaseServerDo> specification, Sort sort);
}
