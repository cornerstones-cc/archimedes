package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserBasicDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface UserBasicRepository extends PagingAndSortingRepository<UserBasicDo, Long> {
    boolean existsByUid(Long uid);

    boolean existsByDisplayName(String displayName);

    UserBasicDo findByUid(Long uid);

    List<UserBasicDo> findByUidIn(List<Long> uidList);

    Page<UserBasicDo> findAll(Specification<UserBasicDo> specification, Pageable pageable);

    List<UserBasicDo> findAll(Specification<UserBasicDo> specification, Sort sort);
}
