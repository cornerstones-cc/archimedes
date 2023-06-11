package cc.cornerstones.arbutus.lock.persistence;

import cc.cornerstones.arbutus.lock.entity.LockDo;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author bbottong
 */
public interface LockRepository extends PagingAndSortingRepository<LockDo, Long> {

    LockDo findByNameAndResourceAndVersion(String name, String resource, Long version);
}
