package cc.cornerstones.almond.utils;

public class AbcPostgresqlUtils {
    /**
     * 处理表名/视图名/列名中的转义符 for Postgresql
     *
     * @param name
     * @return
     */
    public static String buildEscapedName(String name) {
        return "\"" + name + "\"";
    }

    /**
     * 处理取值中的转义符 for Postgresql
     *
     * @param value
     * @return
     */
    public static String buildEscapedValue(String value) {
        if (value.contains("'")) {
            StringBuilder newValue = new StringBuilder();
            for (int j = 0; j < value.length(); j++) {
                String character = value.substring(j, j + 1);
                if (character.equals("'")) {
                    newValue.append("''");
                } else {
                    newValue.append(character);
                }
            }
            return newValue.toString();
        } else {
            return value;
        }
    }

    public static String surroundBySingleQuotes(String value) {
        if (value == null) {
            return null;
        }

        return "'" + value + "'";
    }
}
