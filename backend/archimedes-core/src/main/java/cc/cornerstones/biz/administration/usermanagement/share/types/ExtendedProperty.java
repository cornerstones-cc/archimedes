package cc.cornerstones.biz.administration.usermanagement.share.types;

import lombok.Data;

@Data
public class ExtendedProperty {
    private Long extendedPropertyUid;
    private String extendedPropertyName;
    private Object extendedPropertyValue;
}
