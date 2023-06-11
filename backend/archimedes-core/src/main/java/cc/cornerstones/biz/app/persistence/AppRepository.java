package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppDo;
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
public interface AppRepository extends PagingAndSortingRepository<AppDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByName(String name);

    AppDo findByUid(Long uid);

    Page<AppDo> findAll(Specification<AppDo> specification, Pageable pageable);

    List<AppDo> findAll(Specification<AppDo> specification, Sort sort);

    List<AppDo> findByUidIn(List<Long> uidList);

    List<AppDo> findByEnabledAndUidIn(Boolean enabled, List<Long> uidList, Sort sort);

    @Query("SELECT u.uid FROM AppDo u WHERE u.owner = ?1")
    List<Long> findByOwner(Long owner);
}
