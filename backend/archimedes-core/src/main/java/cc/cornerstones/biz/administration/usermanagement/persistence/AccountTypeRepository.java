package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.AccountTypeDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface AccountTypeRepository extends PagingAndSortingRepository<AccountTypeDo, Long> {
    boolean existsByUid(Long uid);

    AccountTypeDo findByUid(Long uid);

    boolean existsByName(String name);

    AccountTypeDo findByName(String name);

    Page<AccountTypeDo> findAll(Specification<AccountTypeDo> specification, Pageable pageable);

    List<AccountTypeDo> findAll(Specification<AccountTypeDo> specification, Sort sort);
}
