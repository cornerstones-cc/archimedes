package cc.cornerstones.almond.utils;

public class AbcMssqlUtils {
    /**
     * 处理数据库名/Schema名/表名/视图名/列名中的转义符 for MSSQL
     *
     * @param name
     * @return
     */
    public static String buildEscapedName(String name) {
        int indexOfEscapedCharacter = name.indexOf(']');
        if (indexOfEscapedCharacter < 0) {
            return "[" + name + "]";
        }

        StringBuilder newFieldName = new StringBuilder();
        newFieldName.append(name.substring(0, indexOfEscapedCharacter));
        newFieldName.append(']');
        newFieldName.append(name.substring(indexOfEscapedCharacter, name.length()));
        return "[" + newFieldName.toString() + "]";
    }

    /**
     * 处理取值中的转义符 for MSSQL
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
