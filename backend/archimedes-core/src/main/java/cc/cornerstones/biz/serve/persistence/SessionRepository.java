package cc.cornerstones.biz.serve.persistence;

import cc.cornerstones.biz.serve.entity.SessionDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface SessionRepository extends PagingAndSortingRepository<SessionDo, Long> {
    Page<SessionDo> findAll(Specification<SessionDo> specification, Pageable pageable);

    List<SessionDo> findAll(Specification<SessionDo> specification, Sort sort);

    SessionDo findByUid(Long uid);
}
