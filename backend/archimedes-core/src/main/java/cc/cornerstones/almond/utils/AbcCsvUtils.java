package cc.cornerstones.almond.utils;


import com.google.common.base.Strings;
import com.opencsv.CSVReader;
import cc.cornerstones.almond.types.AbcTuple2;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author bbottong
 */
public class AbcCsvUtils {
    private static final int INFER_COLUMN_DATA_TYPE_BY_ANALYZING_N_ROWS = 10;

    /**
     * 从CSV File中解析Header并推测每列的数据类型
     *
     * @param file
     * @param textEncoding
     * @param headerRowNum
     * @param delimiter
     * @return
     * @throws IOException
     */
    public static List<AbcTuple2<String, Class<?>>> extractHeaderAndInferDataTypesFromCsvFile(File file, String
            textEncoding, Integer headerRowNum, String delimiter) throws IOException {
        // 每个header column最终有且只有1个determined data type
        List<AbcTuple2<String, Class<?>>> listOfHeaderAndDeterminedDataType = new LinkedList<>();
        // 每个header column有N个candidate data types
        List<AbcTuple2<String, List<Class<?>>>> listOfHeaderAndCandidateDataType = new LinkedList<>();
        int headerColumnCount = 0;

        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        CSVReader reader = null;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(file), Charset.forName(textEncoding));
            bufferedReader = new BufferedReader(inputStreamReader);
            reader = new CSVReader(bufferedReader,delimiter.charAt(0));
            String[] line;
            int rowNo = 0;
            while ((line = reader.readNext()) != null) {
                if (rowNo < headerRowNum) {
                    rowNo++;
                    continue;
                }
                if (rowNo == headerRowNum) {
                    //
                    // 读取header row，获取header row的列名
                    //
                    headerColumnCount = line.length;
                    for (int i = 0; i < headerColumnCount; i++) {
                        // 默认字符串类型
                        AbcTuple2<String, Class<?>> tuple = new AbcTuple2<>(line[i].trim(), String.class);

                        listOfHeaderAndDeterminedDataType.add(tuple);
                    }
                } else if (rowNo < headerRowNum + INFER_COLUMN_DATA_TYPE_BY_ANALYZING_N_ROWS) {
                    //
                    // 读取header之后的N行，推测每列数据类型
                    //

                    for (int i = 0; i < line.length; i++) {
                        String value = line[i].trim();

                        List<Class<?>> listOfCandidateDataType = null;
                        AbcTuple2<String, List<Class<?>>> tuple = null;
                        if (listOfHeaderAndCandidateDataType.size() > i) {
                            tuple = listOfHeaderAndCandidateDataType.get(i);
                        }
                        if (tuple == null) {
                            listOfCandidateDataType = new LinkedList<>();
                            tuple = new AbcTuple2<>(listOfHeaderAndDeterminedDataType.get(i).f, listOfCandidateDataType);
                            listOfHeaderAndCandidateDataType.add(i, tuple);
                        } else {
                            listOfCandidateDataType = tuple.s;
                        }

                        if (Strings.isNullOrEmpty(value)) {
                            listOfCandidateDataType.add(String.class);
                            continue;
                        }

                        try {
                            // Integer类型
                            Integer.parseInt(value);
                            listOfCandidateDataType.add(Integer.class);
                            continue;
                        } catch (NumberFormatException e1) {
                            // DO NOTHING
                        }

                        try {
                            // Long类型
                            Long.parseLong(value);
                            listOfCandidateDataType.add(Long.class);
                            continue;
                        } catch (NumberFormatException e2) {
                            // DO NOTHING
                        }

                        try {
                            // Float & Double类型
                            Double.parseDouble(value);
                            listOfCandidateDataType.add(Double.class);
                            continue;
                        } catch (NumberFormatException e3) {
                            // DO NOTHING
                        }

                        try {
                            // Boolean类型
                            if ("TRUE".equalsIgnoreCase(value) || "FALSE".equalsIgnoreCase(value)) {
                                listOfCandidateDataType.add(Boolean.class);
                                continue;
                            }
                        } catch (NumberFormatException e4) {
                            // DO NOTHING
                        }

                        // Date类型
                        boolean validDate = false;
                        for (DateTimeFormatter dateFormatter : AbcDateUtils.listOfDateFormatter) {
                            try {
                                DateTime.parse(value, dateFormatter);
                                validDate = true;
                                break;
                            } catch (Exception e) {
                                // DO NOTHING
                            }
                        }
                        if (validDate) {
                            listOfCandidateDataType.add(Date.class);
                            continue;
                        }

                        // Timestamp类型
                        boolean validTimestamp = false;
                        for (DateTimeFormatter timestampFormatter : AbcDateUtils.listOfTimestampFormatter) {
                            try {
                                DateTime.parse(value, timestampFormatter);
                                validTimestamp = true;
                                break;
                            } catch (Exception e) {
                                // DO NOTHING
                            }
                        }
                        if (validTimestamp) {
                            listOfCandidateDataType.add(Timestamp.class);
                            continue;
                        }

                        // 默认String类型
                        listOfCandidateDataType.add(String.class);
                    }
                }

                rowNo++;

                if (rowNo >= headerRowNum + INFER_COLUMN_DATA_TYPE_BY_ANALYZING_N_ROWS) {
                    break;
                }
            }

            //
            // 从每个header的candidate data types中找出出现次数最多的那个data type，作为最终data type
            //
            for (int i = 0; i < listOfHeaderAndCandidateDataType.size(); i++) {
                AbcTuple2<String, List<Class<?>>> tuple = listOfHeaderAndCandidateDataType.get(i);
                List<Class<?>> listOfCandidateDataType = tuple.s;

                Map<Class<?>, Integer> count = new HashMap<>();
                for (Class<?> clazz : listOfCandidateDataType) {
                    if (count.get(clazz) == null) {
                        count.put(clazz, 1);
                    } else {
                        count.put(clazz, count.get(clazz) + 1);
                    }
                }

                Class<?> clazz = null;
                Integer max = 0;

                for (Map.Entry<Class<?>, Integer> entry : count.entrySet()) {
                    if (entry.getValue() > max) {
                        max = entry.getValue();
                        clazz = entry.getKey();
                    }
                }

                listOfHeaderAndDeterminedDataType.get(i).s = clazz;
            }


            return listOfHeaderAndDeterminedDataType;
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                	reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 数字列序转换成从字母A开始的字母列序
     *
     * @param n
     * @return
     */
    public static String convertToTitle(int n) {
        StringBuilder ans = new StringBuilder();
        while (n > 0) {
            int k = n % 26;
            if (k == 0) {
               ans.append('Z'); 
                n -= 26;
            } else {
                char c = (char) ('A' + k - 1);
                ans.append(c);
            }
            n /= 26;
        }
        StringBuilder res = new StringBuilder();
        while (ans.length() > 0) {
            res.append(ans.charAt(ans.length() - 1));
            ans.deleteCharAt(ans.length() - 1);
        }
        return res.toString();
    }
}