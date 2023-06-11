package cc.cornerstones.almond.types;

import lombok.Data;

import java.util.List;

@Data
public class AbcSort {
    private List<AbcOrder> orders;

    public AbcSort() {

    }

    public AbcSort(List<AbcOrder> orders) {
        this.orders = orders;
    }
}
