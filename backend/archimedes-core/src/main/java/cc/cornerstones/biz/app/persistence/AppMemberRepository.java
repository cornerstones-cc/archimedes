package cc.cornerstones.biz.app.persistence;

import cc.cornerstones.biz.app.entity.AppMemberDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface AppMemberRepository extends PagingAndSortingRepository<AppMemberDo, Long> {
    Page<AppMemberDo> findAll(Specification<AppMemberDo> specification, Pageable pageable);

    AppMemberDo findByAppUidAndUserUid(Long appUid, Long userUid);

    List<AppMemberDo> findByAppUidAndUserUidIn(Long appUid, List<Long> userUidList);

    List<AppMemberDo> findAll(Specification<AppMemberDo> specification, Sort sort);

    List<AppMemberDo> findByUserUid(Long userUid);

    List<AppMemberDo> findByAppUid(Long appUid);
}
