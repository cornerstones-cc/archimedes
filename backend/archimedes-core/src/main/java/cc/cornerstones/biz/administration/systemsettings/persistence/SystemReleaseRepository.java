package cc.cornerstones.biz.administration.systemsettings.persistence;

import cc.cornerstones.biz.administration.systemsettings.entity.SystemReleaseDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface SystemReleaseRepository extends PagingAndSortingRepository<SystemReleaseDo, Long> {
    SystemReleaseDo findByUid(Long uid);

    Page<SystemReleaseDo> findAll(Specification<SystemReleaseDo> specification, Pageable pageable);

    List<SystemReleaseDo> findAll(Specification<SystemReleaseDo> specification, Sort sort);
}
