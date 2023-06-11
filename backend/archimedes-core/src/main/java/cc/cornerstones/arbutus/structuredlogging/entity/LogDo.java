package cc.cornerstones.arbutus.structuredlogging.entity;

import cc.cornerstones.almond.types.BaseDo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * @author bbottong
 *
 * 数据源
 */
@EqualsAndHashCode(callSuper=false)
@Data
@Entity
@Table(name = LogDo.RESOURCE_NAME,
        indexes = {
                @Index(name = "job_category_n_job_uid", columnList = "job_category, job_uid", unique = false)
        })
@Where(clause = "is_deleted=0")
public class LogDo extends BaseDo {
    public static final String RESOURCE_NAME = "t9_log";
    public static final String RESOURCE_SYMBOL = "Log";

    /**
     * job category
     */
    @Column(name = "job_category", length = 128)
    private String jobCategory;

    /**
     * job uid
     */
    @Column(name = "job_uid", length = 36)
    private String jobUid;

    /**
     * job instance failed reason
     */
    @Lob
    @Column(name = "content")
    private String content;
}
