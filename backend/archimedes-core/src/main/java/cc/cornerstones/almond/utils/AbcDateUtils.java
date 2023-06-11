package cc.cornerstones.almond.utils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AbcDateUtils {
    public static final String[] ARRAY_OF_DATE_PATTERN = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM-dd-yyyy",
            "MM/dd/yyyy",
            "d MMMM, yyyy",
            "d MMM, yyyy",
            "yyyy年M月d日"
    };

    public static List<DateTimeFormatter> listOfDateFormatter;

    public static List<DateTimeFormatter> listOfTimestampFormatter;

    public static final String[] ARRAY_OF_TIMESTAMP_PATTERN = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "MM-dd-yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss",
            "d MMMM, yyyy HH:mm:ss",
            "d MMM, yyyy HH:mm:ss",
            "yyyy年M月d日 HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy/MM/dd HH:mm:ss.SSS",
            "MM-dd-yyyy HH:mm:ss.SSS",
            "MM/dd/yyyy HH:mm:ss.SSS",
            "d MMMM, yyyy HH:mm:ss.SSS",
            "d MMM, yyyy HH:mm:ss.SSS",
            "yyyy年M月d日 HH:mm:ss.SSS"
    };

    static {
        listOfDateFormatter = new ArrayList<>(ARRAY_OF_DATE_PATTERN.length);
        for (String pattern : ARRAY_OF_DATE_PATTERN) {
            DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(pattern);
            listOfDateFormatter.add(dateFormatter);
        }

        listOfTimestampFormatter = new ArrayList<>(ARRAY_OF_TIMESTAMP_PATTERN.length);
        for (String pattern : ARRAY_OF_TIMESTAMP_PATTERN) {
            DateTimeFormatter dateFormatter = DateTimeFormat.forPattern(pattern);
            listOfTimestampFormatter.add(dateFormatter);
        }
    }

    public static String format(long millis) {
        if (millis <= 0) {
            return "0";
        }

        StringBuilder formatted = new StringBuilder();

        if (millis >= 60 * 60 * 1000) {
            // 按小时，分，格式化
            long hours = millis / (60 * 60 * 1000);
            long mins = (millis - hours * 60 * 60 * 1000) / (60 * 1000);
            if (mins == 0) {
                formatted.append(hours).append("小时");
            } else {
                formatted.append(hours).append("小时").append(mins).append("分钟");
            }
        } else if (millis >= 60 * 1000) {
            // 按分，秒，格式化
            long mins = millis / (60 * 1000);
            long seconds = (millis - mins * 60 * 1000) / 1000;
            if (seconds == 0) {
                formatted.append(mins).append("分钟");
            } else {
                formatted.append(mins).append("分钟").append(seconds).append("秒");
            }
        } else if (millis >= 1000) {
            // 按秒，毫秒格式化
            long seconds = millis / 1000;
            long remainderMillis = millis % 1000;
            if (remainderMillis == 0) {
                formatted.append(seconds).append("秒");
            } else {
                formatted.append(seconds).append("秒").append(remainderMillis).append("毫秒");
            }
        } else {
            formatted.append(millis).append("毫秒");
        }

        return formatted.toString();
    }

    public static Date transform(String timestamp) throws Exception {
        DateTime dateTime = null;
        for (DateTimeFormatter dateTimeFormatter : listOfTimestampFormatter) {
            try {
                dateTime = DateTime.parse(timestamp, dateTimeFormatter);
            } catch (Exception e) {
                // DO NOTHING;
            } finally {
                if (dateTime != null) {
                    return dateTime.toDate();
                }
            }
        }

        throw new Exception("fail to transform to timestamp: " + timestamp);
    }

    public static void main(String args[]) {

        long millis = 3 * 60 * 60 * 1000;
        System.out.println(format(millis));
    }
}
