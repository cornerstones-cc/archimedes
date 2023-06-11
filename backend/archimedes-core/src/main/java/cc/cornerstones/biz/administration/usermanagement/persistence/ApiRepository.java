package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.ApiDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author bbottong
 */
public interface ApiRepository extends PagingAndSortingRepository<ApiDo, Long> {

    Page<ApiDo> findAll(Specification<ApiDo> specification, Pageable pageable);

    List<ApiDo> findAll(Specification<ApiDo> specification, Sort sort);

    List<ApiDo> findAll();
}
