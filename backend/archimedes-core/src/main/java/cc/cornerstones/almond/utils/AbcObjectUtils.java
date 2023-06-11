package cc.cornerstones.almond.utils;

import cc.cornerstones.almond.exceptions.AbcResourceConflictException;
import cc.cornerstones.almond.exceptions.AbcUndefinedException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author bbottong
 */
public class AbcObjectUtils {
    private AbcObjectUtils() {

    }

    public static String toString(Object object) throws Exception {
        if (object instanceof String) {
            return (String) object;
        } else if (object instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format((Date) object);
        } else {
            return String.valueOf(object);
        }
    }

    /**
     * 获取对象的所有属性，包括自己类和祖先类
     *
     * @param object
     * @return
     */
    public static Field[] getAllFields(Object object) {
        Class clazz = object.getClass();
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null) {
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        fieldList.toArray(fields);
        return fields;
    }

    /**
     * 在对象的所有属性（包括自己类和祖先类）中寻找目标属性
     *
     * @param object
     * @return
     */
    public static Field getField(Object object, String fieldName) {
        Class clazz = object.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    return field;
                }
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        return null;
    }

    public static Long toLong(Object object) throws AbcUndefinedException {
        if (object instanceof Short) {
            return ((Short) object).longValue();
        } else if (object instanceof Integer) {
            return ((Integer) object).longValue();
        } else if (object instanceof Long) {
            return (Long) object;
        } else if (object instanceof BigDecimal){
            return ((BigDecimal) object).longValue();
        } else {
            throw new AbcResourceConflictException("unsupported type:" + object);
        }
    }

    public static Integer toInteger(Object object) throws AbcUndefinedException {
        if (object instanceof Short) {
            return ((Short) object).intValue();
        } else if (object instanceof Integer) {
            return ((Integer) object);
        } else if (object instanceof Long) {
            return ((Long) object).intValue();
        } else if (object instanceof BigDecimal){
            return ((BigDecimal) object).intValue();
        } else {
            throw new AbcResourceConflictException("unsupported type:" + object);
        }
    }
}
