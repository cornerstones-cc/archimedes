package cc.cornerstones.biz.administration.serviceconnection.persistence;

import cc.cornerstones.biz.administration.serviceconnection.entity.UserSynchronizationExecutionInstanceDo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author bbottong
 */
public interface UserSynchronizationExecutionInstanceRepository extends PagingAndSortingRepository<UserSynchronizationExecutionInstanceDo, Long> {
    Page<UserSynchronizationExecutionInstanceDo> findAll(
            Specification<UserSynchronizationExecutionInstanceDo> specification,
            Pageable pageable);
}
