package cc.cornerstones.almond.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * @author bbottong
 */
public class AbcNameGeneratorUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbcNameGeneratorUtils.class);

    private static HanyuPinyinOutputFormat defaultOutputFormat = null;

    private static final String ALPHABETIC_AND_DIGIT = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ALPHABETIC_AND_DIGIT_COUNT = ALPHABETIC_AND_DIGIT.length();
    private static final String AVOID_KEYWORD_SUFIX = "_aks";

    private static final Integer TABLE_NAME_MAX_LENGTH = 64;

    private static final Integer TABLE_FIELD_NAME_MAX_LENGTH = 64;

    private static final Random RANDOM = new Random();

    private AbcNameGeneratorUtils() {

    }

    public static String generateTableName(String prefix, String originalName) {
        String latest = null;
        if (prefix == null || prefix.isEmpty()) {
            latest = generate(originalName);
        } else {
            latest = prefix + generate(originalName);
        }

        // MySQL limits length of table/column/index name, ref:
        // https://dev.mysql.com/doc/refman/5.7/en/identifiers.html
        if (latest.length() > TABLE_NAME_MAX_LENGTH) {
            return latest.substring(0, TABLE_NAME_MAX_LENGTH);
        }

        return latest;
    }

    public static String generateTableFieldName(String prefix, String originalName) {
        String latest = null;
        if (prefix == null || prefix.isEmpty()) {
            latest = generate(originalName);
        } else {
            latest = prefix + generate(originalName);
        }

        // MySQL limits length of table/column/index name, ref:
        // https://dev.mysql.com/doc/refman/5.7/en/identifiers.html
        if (latest.length() > TABLE_FIELD_NAME_MAX_LENGTH) {
            return latest.substring(0, TABLE_FIELD_NAME_MAX_LENGTH);
        }

        return latest;
    }

    public static String generate(String original) {
        if (defaultOutputFormat == null) {
            defaultOutputFormat = new HanyuPinyinOutputFormat();
            defaultOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
            defaultOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
            defaultOutputFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        }

        // 如果能匹配数字库关键字就不需要再进行拼音的转化，直接加一个前缀用以区别
        String lowercaseOriginal = original.toLowerCase();
        if (!AbcKeywordUtils.MYSQL_KEYWORDS.contains(lowercaseOriginal) &&
                !AbcKeywordUtils.SQLSERVER_KEYWORDS.contains(lowercaseOriginal)) {
            StringBuilder pinyin = new StringBuilder();

            char[] elements = original.toCharArray();

            for (int i = 0; i < elements.length; i++) {
                char element = elements[i];
                //如果第一位是数字，自动拼接前缀
                if (i == 0 && Character.isDigit(element)) {
                    pinyin.append(AVOID_KEYWORD_SUFIX);
                }
                // 处理双字节字符
                if (Character.toString(element).matches("[^\\x00-\\xff]")) {
                    // 处理汉字
                    if (Character.toString(element).matches("[\u4e00-\u9fa5]")) {
                        try {
                            String[] srcArray = PinyinHelper.toHanyuPinyinStringArray(element, defaultOutputFormat);
                            if (i == elements.length - 1) {
                                pinyin.append(srcArray[0].toLowerCase());
                            } else {
                                pinyin.append(srcArray[0].toLowerCase() + "_");
                            }
                        } catch (BadHanyuPinyinOutputFormatCombination e) {
                            LOGGER.warn("fail to translate Han to Pinyin: " + Character.toString(element), e);

                            // 用随机字符编码
                            char encoded = ALPHABETIC_AND_DIGIT.charAt(RANDOM.nextInt(ALPHABETIC_AND_DIGIT_COUNT));
                            pinyin.append(encoded);
                        }
                    } else {
                        // 处理非汉字
                        if (!pinyin.toString().endsWith("_") && pinyin.length() > 0) {
                            pinyin.append("_");
                        }
                    }
                } else if (Character.isAlphabetic(element)) {
                    // 处理字母
                    pinyin.append(Character.toLowerCase(element));
                } else if (Character.isDigit(element)) {
                    // 处理数字
                    pinyin.append(Character.toString(element));
                } else if (Character.isWhitespace(element) || element == '_' || element == '-' || element == '.'
                        || element == '|' || element == '/' || element == '\\' || element == '[' || element == ']'
                        || element == '(' || element == ')' || element == '{' || element == '}') {
                    // 处理连接符
                    if (!pinyin.toString().endsWith("_")) {
                        pinyin.append("_");
                    }
                } else {
                    // 余下的全部用随机字符编码
                    char encoded = ALPHABETIC_AND_DIGIT.charAt(RANDOM.nextInt(ALPHABETIC_AND_DIGIT_COUNT));
                    pinyin.append(encoded);
                }
            }

            String result = pinyin.toString();
            if (result.length() > 1 && result.startsWith("_")) {
                result = result.substring(1, result.length());
            }

            if (result.length() > 1 && result.endsWith("_")) {
                result = result.substring(0, result.length() - 1);
            }

            return result;
        } else {
    return AVOID_KEYWORD_SUFIX + lowercaseOriginal;
}

    }

    public static void main(String[] args) {

    }
}
