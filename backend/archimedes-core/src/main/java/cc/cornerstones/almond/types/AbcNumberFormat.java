package cc.cornerstones.almond.types;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class AbcNumberFormat {
    private Category category;
    private JSONObject format;

    public enum Category {
        GENERAL("General"),
        NUMBER("Number"),
        CURRENCY("Currency"),
        ACCOUNTING("Accounting"),
        DATE("Date"),
        TIME("Time"),
        PERCENTAGE("Percentage"),
        FRACTION("Fraction"),
        SCIENTIFIC("Scientific"),
        TEXT("Text"),
        SPECIAL("Special"),
        CUSTOM("Custom");

        private final String symbol;

        Category(String symbol) {
            this.symbol = symbol;
        }
    }

    public static class Number {
        private Integer decimalPlaces;

        /**
         * Use 1000 Separator (,)
         */
        private Boolean useThousandSeparator;

        /**
         * Negative numbers
         * 1,
         * 2,
         * 3,
         * 4,
         */
        private Integer negativeNumbers;
    }

    public static class Currency {
        private Integer decimalPlaces;

        private String symbol;

        /**
         * Negative numbers
         * 1,
         * 2,
         * 3,
         * 4,
         */
        private Integer negativeNumbers;
    }

    public static class Accounting {
        private Integer decimalPlaces;

        private String symbol;
    }
}
