package cc.cornerstones.biz.administration.systemsettings.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * System release
 *
 */
@TinyId(bizType = SystemReleaseDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = SystemReleaseDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class SystemReleaseDo extends BaseDo {
    public static final String RESOURCE_NAME = "k1_system_release";
    public static final String RESOURCE_SYMBOL = "System release";

    /**
     * UID
     */
    @Column(name = "uid")
    private Long uid;

    @Column(name = "system_name", length = 100)
    private String systemName;

    @Column(name = "system_logo_url", length = 200)
    private String systemLogoUrl;

    @Column(name = "big_picture_url", length = 200)
    private String bigPictureUrl;

    @Column(name = "terms_of_service_url", length = 200)
    private String termsOfServiceUrl;

    @Column(name = "privacy_policy_url", length = 200)
    private String privacyPolicyUrl;

    @Column(name = "release_version", length = 45)
    private String releaseVersion;

    @Column(name = "vendor_name", length = 100)
    private String vendorName;
}
