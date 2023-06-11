package cc.cornerstones.almond.types;

import lombok.Data;
import org.springframework.data.domain.Sort;

@Data
public class AbcOrder {
    private String property;
    private Sort.Direction direction;

    public AbcOrder() {

    }

    public AbcOrder(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }
}
