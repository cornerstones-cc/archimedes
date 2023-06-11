package cc.cornerstones.almond.types;

import lombok.Data;

import java.util.List;

@Data
public class AbcPagination {
    private Integer page;
    private Integer size;

    public AbcPagination() {
    }

    public AbcPagination(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }
}
