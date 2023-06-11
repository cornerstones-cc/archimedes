package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.UserCredentialDo;
import org.springframework.data.repository.PagingAndSortingRepository;


/**
 * @author bbottong
 */
public interface UserCredentialRepository extends PagingAndSortingRepository<UserCredentialDo, Long> {
    UserCredentialDo findByUserUid(Long userUid);
}
