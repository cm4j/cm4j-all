package com.cm4j.config;

import java.util.HashMap;
import java.util.Map;

/**
 * 错误码
 */
public enum ErrorCode {

    /**
     * 0-成功
     */
    SUCCESS("0", "成功", "3"),

    ;

    private final String code;
    private final String mess;
    private final short shortCode;
    private final String showType;

    ErrorCode(String code, String mess) {
        this(code, mess, "0");
    }

    ErrorCode(String code, String mess, String showType) {
        this.code = code;
        this.mess = mess;
        this.shortCode = Short.parseShort(code);
        this.showType = showType;
    }

    private static final Map<String, ErrorCode> MAP = new HashMap<>();

    static {
        for (ErrorCode code : values()) {
            MAP.put(code.getCode(), code);
        }
    }

    public static ErrorCode getErrorCode(String errorCode) {
        return MAP.get(errorCode);
    }

    public String getMess() {
        return mess;
    }

    public String getCode() {
        return code;
    }

    public String getShowType() {
        return showType;
    }

    public short getShortCode() {
        return shortCode;
    }


}
