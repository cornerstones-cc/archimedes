package cc.cornerstones.biz.administration.usermanagement.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * API
 *
 * @author bbottong
 */
@TinyId(bizType = ApiDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = ApiDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class ApiDo extends BaseDo {
    public static final String RESOURCE_NAME = "k2_permission_function_api";
    public static final String RESOURCE_SYMBOL = "Api";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    /**
     * URI
     */
    @Column(name = "uri", length = 255)
    private String uri;

    /**
     * Method
     */
    @Column(name = "method", length = 10)
    private String method;

    /**
     * Summary
     */
    @Column(name = "summary", length = 255)
    private String summary;

    /**
     * Tag
     */
    @Column(name = "tag")
    private String tag;
}