package cc.cornerstones.biz.app.entity;

import cc.cornerstones.almond.types.BaseDo;
import cc.cornerstones.arbutus.tinyid.service.TinyId;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;

import javax.persistence.*;

/**
 * App open api credential
 *
 * @author bbottong
 */

@TypeDef(name = "json", typeClass = JsonStringType.class)
@TinyId(bizType = AppOpenApiCredentialDo.RESOURCE_NAME, beginId = 100, maxId = 100, step = 100, remainder = 0, delta
        = 1)
@EqualsAndHashCode(callSuper = false)
@Data
@Entity
@Table(name = AppOpenApiCredentialDo.RESOURCE_NAME)
@Where(clause = "is_deleted=0")
public class AppOpenApiCredentialDo extends BaseDo {
    public static final String RESOURCE_NAME = "f1_app_open_api_credential";
    public static final String RESOURCE_SYMBOL = "App open api credential";

    /**
     * App key
     */
    @Column(name = "app_key", length = 64)
    private String appKey;

    /**
     * App secret
     */
    @Column(name = "app_secret", length = 64)
    private String appSecret;

    /**
     * User (organization) uid
     */
    @Column(name = "user_uid")
    private Long userUid;

    /**
     * 所属 App 的 UID
     */
    @Column(name = "app_uid")
    private Long appUid;
}