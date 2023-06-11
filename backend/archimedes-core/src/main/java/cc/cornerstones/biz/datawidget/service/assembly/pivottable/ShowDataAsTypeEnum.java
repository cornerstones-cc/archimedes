package cc.cornerstones.biz.datawidget.service.assembly.pivottable;

public enum ShowDataAsTypeEnum {
    NO_CALCULATION("No Calculation"),
    DIFFERENCE_FROM("Difference From"),
    PERCENTAGE_OF("% Of"),
    PERCENTAGE_DIFFERENCE_FROM("% Difference From"),
    RUNNING_TOTAL_IN("Running Total In"),
    PERCENTAGE_OF_ROW_TOTAL("% of Row Total"),
    PERCENTAGE_OF_COLUMN_TOTAL("% of Column Total"),
    PERCENTAGE_OF_GRAND_TOTAL("% of Grand Total"),
    INDEX("Index"),
    PERCENTAGE_OF_PARENT_ROW_TOTAL("% of Parent Row Total"),
    PERCENTAGE_OF_PARENT_COLUMN_TOTAL("% of Parent Column Total"),
    PERCENTAGE_OF_PARENT_TOTAL("% of Parent Total"),
    PERCENTAGE_RUNNING_TOTAL_IN("% Running Total In"),
    RANK_SMALLEST_TO_LARGEST("Rank Smallest to Largest"),
    RANK_LARGEST_TO_SMALLEST("Rank Largest to Smallest");

    private final String symbol;

    ShowDataAsTypeEnum(String symbol) {
        this.symbol = symbol;
    }
}
