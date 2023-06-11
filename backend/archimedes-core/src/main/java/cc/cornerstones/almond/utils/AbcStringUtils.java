package cc.cornerstones.almond.utils;

import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AbcStringUtils {
    private static final Pattern pattern = Pattern.compile("[A-Z]");

    private static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("^[a-z_]+[a-z0-9_]*$");

    private AbcStringUtils() {

    }

    /**
     * 驼峰转下划线
     *
     * @param str
     * @return
     */
    public static String camelCaseToSnakeCase(String str) {
        // Regular Expression
        String regex = "([a-z])([A-Z]+)";

        // Replacement string
        String replacement = "$1_$2";

        // Replace the given regex
        // with replacement string
        // and convert it to lower case.
        str = str.replaceAll(regex, replacement).toLowerCase();

        // return string
        return str;
    }

    /**
     * 下划线转驼峰 (首字母小写)
     *
     * @param str
     * @return
     */
    public static String snakeCaseToCamelCase(String str) {
        // Lower case first letter of string
        str = str.substring(0, 1).toLowerCase() + str.substring(1);

        // Run a loop till string
        // string contains underscore
        while (str.contains("_")) {

            // Replace the first occurrence
            // of letter that present after
            // the underscore, to capitalize
            // form of next letter of underscore
            String newStr = str
                    .replaceFirst(
                            "_[a-zA-Z0-9]",
                            String.valueOf(
                                    Character.toUpperCase(
                                            str.charAt(
                                                    str.indexOf("_") + 1))));
            if  (newStr.equals(str)) {
                break;
            }

            str = newStr;
        }

        // Return string
        return str;
    }

    /**
     * 下划线转驼峰 (首字母大写)
     *
     * @param str
     * @return
     */
    public static String anotherSnakeCaseToCamelCase(String str) {
        // Capitalize first letter of string
        str = str.substring(0, 1).toUpperCase() + str.substring(1);

        // Run a loop till string
        // string contains underscore
        while (str.contains("_")) {

            // Replace the first occurrence
            // of letter that present after
            // the underscore, to capitalize
            // form of next letter of underscore
            String newStr = str
                    .replaceFirst(
                            "_[a-zA-Z0-9]",
                            String.valueOf(
                                    Character.toUpperCase(
                                            str.charAt(
                                                    str.indexOf("_") + 1))));
            if  (newStr.equals(str)) {
                break;
            }

            str = newStr;
        }

        // Return string
        return str;
    }

    public static boolean isLegalKey(String key) {
        Matcher matcher = LEGAL_KEY_PATTERN.matcher(key);
        return matcher.find();
    }

    public static String toString(List<?> list, String delimiter) {
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        if (delimiter == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (list.size() == 1) {
            if (list.get(0) instanceof String) {
                return (String) list.get(0);
            } else {
                return String.valueOf(list.get(0));
            }
        }

        stringBuilder.append(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            stringBuilder.append(delimiter).append(list.get(i));
        }

        return stringBuilder.toString();
    }

    public static String toString(Map<?, ?> map, String delimiter) {
        if (CollectionUtils.isEmpty(map)) {
            return null;
        }
        if (delimiter == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();

        map.forEach((key, value) -> {
            stringBuilder.append("(").append(key).append(",").append(value).append(")").append(delimiter);
        });

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }
}
