package com.global.ums.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.global.ums.enums.DataType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * 数据类型推断工具类
 * 用于根据数据内容自动推断数据类型
 */
public class DataTypeUtils {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern LONG_PATTERN = Pattern.compile("^-?\\d+[lL]?$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^-?\\d+\\.\\d+[fF]?$");
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("^-?\\d+\\.\\d+[dD]?$");
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATETIME_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}|\\d{4}/\\d{2}/\\d{2})\\s*(\\d{2}:\\d{2}:\\d{2})?$"
    );

    /**
     * 根据字节数组推断数据类型
     */
    public static DataType inferDataType(byte[] data) {
        if (data == null || data.length == 0) {
            return DataType.UNKNOWN;
        }

        // 检查是否为二进制数据（文件、图片等）
        if (isBinaryData(data)) {
            return DataType.BINARY;
        }

        // 尝试转换为字符串
        String strValue = new String(data, StandardCharsets.UTF_8);
        return inferDataTypeFromString(strValue);
    }

    /**
     * 判断是否为二进制数据
     * 通过检查字节数组是否包含不可打印字符来判断
     */
    private static boolean isBinaryData(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }

        // 检查文件头（魔数）来判断文件类型
        if (isKnownFileType(data)) {
            return true;
        }

        // 检查前几个字节，如果包含不可打印字符，认为是二进制数据
        int checkLength = Math.min(data.length, 100); // 只检查前100个字节
        for (int i = 0; i < checkLength; i++) {
            byte b = data[i];
            // 检查是否为不可打印字符（除了空格、制表符、换行符等）
            if (b < 32 && b != 9 && b != 10 && b != 13) { // 9=tab, 10=LF, 13=CR
                return true;
            }
        }

        // 检查是否包含大量不可打印字符
        int nonPrintableCount = 0;
        for (int i = 0; i < checkLength; i++) {
            byte b = data[i];
            if (b < 32 || b > 126) { // 可打印ASCII字符范围
                nonPrintableCount++;
            }
        }

        // 如果不可打印字符超过30%，认为是二进制数据
        return (double) nonPrintableCount / checkLength > 0.3;
    }

    /**
     * 检查是否为已知的文件类型（通过文件头判断）
     */
    private static boolean isKnownFileType(byte[] data) {
        if (data.length < 4) {
            return false;
        }

        // 检查常见的文件类型魔数
        // JPEG
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8 && data[2] == (byte) 0xFF) {
            return true;
        }
        
        // PNG
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return true;
        }
        
        // GIF
        if ((data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46) ||
            (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x38)) {
            return true;
        }
        
        // PDF
        if (data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46) {
            return true;
        }
        
        // ZIP/RAR/7Z等压缩文件
        if (data[0] == 0x50 && data[1] == 0x4B) { // ZIP
            return true;
        }
        if (data[0] == 0x52 && data[1] == 0x61 && data[2] == 0x72 && data[3] == 0x21) { // RAR
            return true;
        }
        
        // Office文档
        if (data[0] == 0x50 && data[1] == 0x4B && data[2] == 0x03 && data[3] == 0x04) { // DOCX, XLSX, PPTX
            return true;
        }
        return false;
    }

    /**
     * 根据字符串推断数据类型
     */
    public static DataType inferDataTypeFromString(String value) {
        if (StringUtils.isEmpty(value)) {
            return DataType.UNKNOWN;
        }

        // 去除首尾空格
        value = value.trim();

        // 检查是否为布尔值
        if (BOOLEAN_PATTERN.matcher(value).matches()) {
            return DataType.BOOLEAN;
        }

        // 检查是否为长整数
        if (LONG_PATTERN.matcher(value).matches()) {
            // 检查是否超出int范围
            try {
                long longValue = Long.parseLong(value.replaceAll("[lL]$", ""));
                if (longValue > Integer.MAX_VALUE || longValue < Integer.MIN_VALUE) {
                    return DataType.LONG;
                } else {
                    return DataType.INTEGER;
                }
            } catch (NumberFormatException e) {
                return DataType.LONG;
            }
        }

        // 检查是否为整数
        if (INTEGER_PATTERN.matcher(value).matches()) {
            return DataType.INTEGER;
        }

        // 检查是否为双精度浮点数
        if (DOUBLE_PATTERN.matcher(value).matches()) {
            return DataType.DOUBLE;
        }

        // 检查是否为单精度浮点数
        if (FLOAT_PATTERN.matcher(value).matches()) {
            return DataType.FLOAT;
        }

        // 检查是否为日期时间
        if (DATETIME_PATTERN.matcher(value).matches()) {
            return DataType.DATETIME;
        }

        // 检查是否为JSON对象
        if (isValidJsonObject(value)) {
            return DataType.JSON;
        }

        // 检查是否为JSON数组
        if (isValidJsonArray(value)) {
            return DataType.ARRAY;
        }

        // 默认为字符串类型
        return DataType.STRING;
    }

    /**
     * 检查是否为有效的JSON对象
     */
    private static boolean isValidJsonObject(String value) {
        try {
            JSONObject.parseObject(value);
            return value.startsWith("{") && value.endsWith("}");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为有效的JSON数组
     */
    private static boolean isValidJsonArray(String value) {
        try {
            JSONArray.parseArray(value);
            return value.startsWith("[") && value.endsWith("]");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据数据类型将字节数组转换为对应的Java对象
     */
    public static Object convertToTypedValue(byte[] data, DataType dataType) {
        if (data == null || data.length == 0) {
            return null;
        }

        String strValue = new String(data, StandardCharsets.UTF_8);
        
        switch (dataType) {
            case STRING:
                return strValue;
                
            case INTEGER:
                try {
                    return Integer.parseInt(strValue);
                } catch (NumberFormatException e) {
                    return null;
                }
                
            case LONG:
                try {
                    return Long.parseLong(strValue.replaceAll("[lL]$", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
                
            case FLOAT:
                try {
                    return Float.parseFloat(strValue.replaceAll("[fF]$", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
                
            case DOUBLE:
                try {
                    return Double.parseDouble(strValue.replaceAll("[dD]$", ""));
                } catch (NumberFormatException e) {
                    return null;
                }
                
            case BOOLEAN:
                return Boolean.parseBoolean(strValue);
                
            case JSON:
                try {
                    return JSONObject.parseObject(strValue);
                } catch (Exception e) {
                    return null;
                }
                
            case ARRAY:
                try {
                    return JSONArray.parseArray(strValue);
                } catch (Exception e) {
                    return null;
                }
                
            case DATETIME:
                try {
                    // 尝试多种日期格式
                    String[] patterns = {
                        "yyyy-MM-dd HH:mm:ss",
                        "yyyy-MM-dd",
                        "yyyy/MM/dd HH:mm:ss",
                        "yyyy/MM/dd"
                    };
                    
                    for (String pattern : patterns) {
                        try {
                            return LocalDateTime.parse(strValue, DateTimeFormatter.ofPattern(pattern));
                        } catch (DateTimeParseException ignored) {
                            // 继续尝试下一个格式
                        }
                    }
                    return null;
                } catch (Exception e) {
                    return null;
                }
                
            case BINARY:
            case UNKNOWN:
            default:
                return data; // 返回原始字节数组
        }
    }

    /**
     * 将Java对象转换为字节数组
     */
    public static byte[] convertToBytes(Object value, DataType dataType) {
        if (value == null) {
            return new byte[0];
        }

        switch (dataType) {
            case STRING:
                return value.toString().getBytes(StandardCharsets.UTF_8);
                
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
                return value.toString().getBytes(StandardCharsets.UTF_8);
                
            case JSON:
            case ARRAY:
                return JSON.toJSONString(value).getBytes(StandardCharsets.UTF_8);
                
            case DATETIME:
                if (value instanceof LocalDateTime) {
                    return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            .getBytes(StandardCharsets.UTF_8);
                }
                return value.toString().getBytes(StandardCharsets.UTF_8);
                
            case BINARY:
                if (value instanceof byte[]) {
                    return (byte[]) value;
                }
                return value.toString().getBytes(StandardCharsets.UTF_8);
                
            case UNKNOWN:
            default:
                return value.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
} 