package cc.cornerstones.biz.administration.usermanagement.persistence;

import cc.cornerstones.biz.administration.usermanagement.entity.NavigationMenuDo;
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
public interface NavigationMenuRepository extends PagingAndSortingRepository<NavigationMenuDo, Long> {
    boolean existsByUid(Long uid);


    @Query("SELECT COUNT(u) FROM NavigationMenuDo u WHERE u.name = ?1 AND u.parentUid IS NULL")
    Integer existsByNameWithoutParent(String name);

    @Query("SELECT COUNT(u) FROM NavigationMenuDo u WHERE u.name = ?1 AND u.parentUid = ?2")
    Integer existsByNameWithinParent(String name, Long parentUid);

    NavigationMenuDo findByUid(Long uid);

    List<NavigationMenuDo> findByParentUid(Long parentUid);

    @Query("SELECT u FROM NavigationMenuDo u WHERE u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<NavigationMenuDo> findAllWithoutParent();

    @Query("SELECT u FROM NavigationMenuDo u WHERE u.parentUid = ?1 ORDER BY u.sequence ASC")
    List<NavigationMenuDo> findAllWithinParent(Long parentUid);

    @Query("SELECT u FROM NavigationMenuDo u WHERE u.sequence > ?1 AND u.parentUid IS NULL ORDER BY u.sequence ASC")
    List<NavigationMenuDo> findAfterSequenceWithoutParent(Float sequence);

    @Query("SELECT u FROM NavigationMenuDo u WHERE u.sequence > ?1 AND u.parentUid = ?2 ORDER BY u.sequence ASC")
    List<NavigationMenuDo> findAfterSequenceWithinParent(Float sequence, Long parentUid);

    Page<NavigationMenuDo> findAll(Specification<NavigationMenuDo> specification, Pageable pageable);

    List<NavigationMenuDo> findAll(Specification<NavigationMenuDo> specification, Sort sort);

    @Query("SELECT MAX(u.sequence) FROM NavigationMenuDo u WHERE u.parentUid IS NULL")
    Float findMaxSequenceWithoutParent();

    @Query("SELECT MAX(u.sequence) FROM NavigationMenuDo u WHERE u.parentUid = ?1")
    Float findMaxSequenceWithinParent(Long parentUid);
}
