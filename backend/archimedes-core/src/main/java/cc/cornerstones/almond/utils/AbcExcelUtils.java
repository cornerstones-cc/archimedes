package cc.cornerstones.almond.utils;

import cc.cornerstones.biz.datafacet.share.constants.DataFieldTypeEnum;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.metadata.CellData;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.AbstractVerticalCellStyleStrategy;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AbcExcelUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbcExcelUtils.class);

    public static final int EXCEL_SHEET_MAX_ROWS_COUNT = 1048576 - 1;

    public static final String EXCEL_DEFAULT_NAME_SUFFIX = ".xlsx";

    public static void removeRowsAfterRowNum(
            File sourceFile,
            File targetFile,
            Integer rowNum,
            Integer sheetNum) throws Exception {
        String temp = sourceFile.getName().toLowerCase();
        if (temp.endsWith(EXCEL_DEFAULT_NAME_SUFFIX)) {
            xlsxRemoveRowsAfterRowNum(sourceFile, targetFile, rowNum, sheetNum);
        } else {
            xlsRemoveRowsAfterRowNum(sourceFile, targetFile, rowNum, sheetNum);
        }
    }

    public static void xlsxRemoveRowsAfterRowNum(
            File sourceFile,
            File targetFile,
            Integer rowNum,
            Integer sheetNum) throws Exception {
        XSSFWorkbook workbook = null;
        FileOutputStream fos = null;
        try {
            // 慎用！！！打开一个8MB的含Pivot的Excel曾导致OOM。
            workbook = new XSSFWorkbook(sourceFile);
            XSSFSheet sheet = workbook.getSheetAt(sheetNum);

            int lastRowNum = sheet.getLastRowNum();
            for (int i = rowNum + 1; i <= lastRowNum; i++) {
                XSSFRow row = sheet.getRow(i);
                sheet.removeRow(row);
            }

            fos = new FileOutputStream(targetFile);

            workbook.write(fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    public static void xlsRemoveRowsAfterRowNum(
            File sourceFile,
            File targetFile,
            Integer rowNum,
            Integer sheetNum) throws Exception {
        HSSFWorkbook workbook = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(sourceFile);
            workbook = new HSSFWorkbook(fis);
            HSSFSheet sheet = workbook.getSheetAt(sheetNum);

            int lastRowNum = sheet.getLastRowNum();
            for (int i = rowNum + 1; i <= lastRowNum; i++) {
                HSSFRow row = sheet.getRow(i);
                sheet.removeRow(row);
            }

            fos = new FileOutputStream(targetFile);

            workbook.write(fos);
        } finally {
            if (fis != null) {
                fis.close();
            }
            if (fos != null) {
                fos.close();
            }
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    public static void main(String[] args) {
        tc1();
    }

    public static void tc1() {
        List<List<String>> head = new ArrayList<List<String>>(1);
        List<String> headLine0 = new LinkedList<>();
        headLine0.add("名称");
        List<String> headLine1 = new LinkedList<>();
        headLine1.add("整数");
        List<String> headLine2 = new LinkedList<>();
        headLine2.add("长整数");
        List<String> headLine3 = new LinkedList<>();
        headLine3.add("长整数（千位半角逗号分隔）");
        List<String> headLine4 = new LinkedList<>();
        headLine4.add("浮点数（小数点2位）");
        List<String> headLine5 = new LinkedList<>();
        headLine5.add("浮点数（小数点4位）");
        List<String> headLine6 = new LinkedList<>();
        headLine6.add("浮点数（默认格式）");
        List<String> headLine7 = new LinkedList<>();
        headLine7.add("金额（人民币）");
        List<String> headLine8 = new LinkedList<>();
        headLine8.add("日期（年/月/日）");
        head.add(headLine0);
        head.add(headLine1);
        head.add(headLine2);
        head.add(headLine3);
        head.add(headLine4);
        head.add(headLine5);
        head.add(headLine6);
        head.add(headLine7);
        head.add(headLine8);

        List<List<Object>> body = new LinkedList<>();

        List<Object> bodyLine0 = new LinkedList<>();
        bodyLine0.add("k1");
        bodyLine0.add("100");
        bodyLine0.add("1234567890");
        bodyLine0.add("1234567890");
        bodyLine0.add("123.4567");
        bodyLine0.add("1234.567891");
        bodyLine0.add("1234.5678");
        bodyLine0.add("3456.99");
        bodyLine0.add("2022/05/06");
        body.add(bodyLine0);

        List<Object> bodyLine1 = new LinkedList<>();
        bodyLine1.add("k2");
        bodyLine1.add(90);
        bodyLine1.add(9876543210L);
        bodyLine1.add(9876543210L);
        bodyLine1.add(1234.567d);
        bodyLine1.add(1234.567891d);
        bodyLine1.add(1234.5678d);
        BigDecimal value = new BigDecimal("381234.56789");
        bodyLine1.add(value);
        bodyLine1.add("2022/07/30 12:13:14");
        body.add(bodyLine1);

        String fileName = "/Users/bbottong/Downloads/"
                + "excel_data_format_test_" + System.currentTimeMillis() + ".xlsx";


        WriteHandler customWriterHandler = new CustomVerticalCellStyle();

        EasyExcel.write(fileName)
                .registerWriteHandler(customWriterHandler)
                //.registerWriteHandler(new CustomCellWriteHandler())
                .head(head)
                .sheet(0)
                .doWrite(body);
    }

    public static class CustomVerticalCellStyle extends AbstractVerticalCellStyleStrategy {
        /**
         * Returns the column width corresponding to each column head
         *
         * @param head Nullable
         * @return
         */
        @Override
        protected WriteCellStyle headCellStyle(Head head) {
            return null;
        }

        /**
         * Returns the column width corresponding to each column head
         *
         * @param head Nullable
         * @return
         */
        @Override
        protected WriteCellStyle contentCellStyle(Head head) {
            int columnIndex = head.getColumnIndex();
            if (columnIndex == 4) {
                WriteCellStyle writeCellStyle = new WriteCellStyle();
                writeCellStyle.setDataFormat((short)2);
                writeCellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
                writeCellStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());

                return writeCellStyle;
            }
            return new WriteCellStyle();
        }
    }

    public static class CustomCellWriteHandler implements CellWriteHandler {
        private CellStyle numberCellStyleWithoutDecimalPoint;
        private CellStyle numberCellStyleWithTwoDecimalPoints;


        /**
         * Called before create the cell
         *
         * @param writeSheetHolder
         * @param writeTableHolder Nullable.It is null without using table writes.
         * @param row
         * @param head             Nullable.It is null in the case of fill data and without head.
         * @param columnIndex
         * @param relativeRowIndex Nullable.It is null in the case of fill data.
         * @param isHead
         */
        @Override
        public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Row row, Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {

        }

        /**
         * Called after the cell is created
         *
         * @param writeSheetHolder
         * @param writeTableHolder Nullable.It is null without using table writes.
         * @param cell
         * @param head             Nullable.It is null in the case of fill data and without head.
         * @param relativeRowIndex Nullable.It is null in the case of fill data.
         * @param isHead
         */
        @Override
        public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
            if (isHead) { // 如果是头，设置对应格式
                // ignore
            } else {
                if (numberCellStyleWithoutDecimalPoint == null) {
                    Workbook workbook = writeSheetHolder.getSheet().getWorkbook(); // 获取 Workbook对象
                    numberCellStyleWithoutDecimalPoint = workbook.createCellStyle(); // 创建一个 CellStyle对象

                    // 设置内容行列对应格式
                    short dataFormat = workbook.createDataFormat()
                            .getFormat(BuiltinFormats.getBuiltinFormat(1));
                    numberCellStyleWithoutDecimalPoint.setDataFormat(dataFormat);
                }
                if (numberCellStyleWithTwoDecimalPoints == null) {
                    Workbook workbook = writeSheetHolder.getSheet().getWorkbook(); // 获取 Workbook对象
                    numberCellStyleWithTwoDecimalPoints = workbook.createCellStyle(); // 创建一个 CellStyle对象

                    // 设置内容行列对应格式
                    short dataFormat = workbook.createDataFormat()
                            .getFormat(BuiltinFormats.getBuiltinFormat(2));
                    numberCellStyleWithTwoDecimalPoints.setDataFormat(dataFormat);

                    Font font = workbook.createFont();
                    font.setFontName("宋体");
                    font.setFontHeightInPoints((short) 24);
                    font.setBold(true);
                    numberCellStyleWithTwoDecimalPoints.setFont(font);
                }

                if (cell.getColumnIndex() == 4) {
                    cell.setCellType(CellType.NUMERIC);
                    cell.setCellStyle(numberCellStyleWithTwoDecimalPoints);
                }
            }
        }

        /**
         * Called after the cell data is converted
         *
         * @param writeSheetHolder
         * @param writeTableHolder Nullable.It is null without using table writes.
         * @param cellData         Nullable.It is null in the case of add header.
         * @param cell
         * @param head             Nullable.It is null in the case of fill data and without head.
         * @param relativeRowIndex Nullable.It is null in the case of fill data.
         * @param isHead
         */
        @Override
        public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, CellData cellData, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

        }

        /**
         * Called after all operations on the cell have been completed
         *
         * @param writeSheetHolder
         * @param writeTableHolder Nullable.It is null without using table writes.
         * @param cellDataList     Nullable.It is null in the case of add header.There may be several when fill the data.
         * @param cell
         * @param head             Nullable.It is null in the case of fill data and without head.
         * @param relativeRowIndex Nullable.It is null in the case of fill data.
         * @param isHead
         */
        @Override
        public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder, List<CellData> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {

        }


    }
}
