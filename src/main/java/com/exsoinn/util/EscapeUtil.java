package com.exsoinn.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulate logic to "escape" characters that create issues when trying to form JSON from strings that contain
 * such characters. This class contains methods for both escaping and unescaping, {@link EscapeUtil#escapeSpecialCharacters(String)}
 * and {@link EscapeUtil#unescapeSpecialCharacters(String)} respectively.
 *
 * Created by QuijadaJ on 8/15/2017.
 */
public class EscapeUtil {
    /*
     * Escape character substitutes
     */
    public static final String ESCAPE_SPACE = "__SPACE__";
    public static final String ESCAPE_EQ_SIGN = "__EQ__";
    public static final String ESCAPE_SEMI_COLON = "__SC__";
    public static final String ESCAPE_DOUBLE_PIPE = "__DP__";
    public static final String ESCAPE_COLON = "__COL__";
    public static final String ESCAPE_DASH = "__DASH__";
    public static final String ESCAPE_SQR_BRCK_OPEN = "__SBO__";
    public static final String ESCAPE_SQR_BRCK_CLOSE = "__SBC__";
    public static final String ESCAPE_BACK_SLASH = "__BS__";
    public static final String ESCAPE_FORWARD_SLASH = "__FS__";

    //Newly Added
    public static final String ESCAPE_QUESTION_MARK = "__QM__";
    public static final String ESCAPE_ASTERISK = "__ASTRK__";
    public static final String ESCAPE_CURLY_BRCK_OPEN = "__CRBO__";
    public static final String ESCAPE_CURLY_BRCK_CLOSE = "__CRBC__";
    public static final String ESCAPE_OPEN_BRCK = "__OPB__";
    public static final String ESCAPE_CLS_BRCK = "__CLB__";
    public static final String ESCAPE_COMMA = "__CM__";
    public static final String ESCAPE_AMPERSAND = "__AMBR__";
    public static final String ESCAPE_PLUS = "__PLS__";
    public static final String ESCAPE_AT_MRK = "__ATM__";
    public static final String ESCAPE_HASH_MRK = "__HSH__";
    public static final String ESCAPE_SINGLE_PIPE = "__SPP__";
    public static final String ESCAPE_TILT_MRK = "__TLT__";
    public static final String ESCAPE_PRCNTG_MRK = "__PRCN__";
    public static final String ESCAPE_CAP_MRK = "__CAP__";


    /**
     * Stores characters and what they're replacement should be within the Rule API. Escaping
     * characters is sometimes required in order to be able to build a {@link com.exsoinn.util.epf.Context} object,
     * which the Rule API heavily depends on.
     */
    public enum EscapeToken {
        SPACE(" ", ESCAPE_SPACE, " "),
        EQUAL_SIGN("=", ESCAPE_EQ_SIGN, "="),
        SEMI_COLON(";", ESCAPE_SEMI_COLON, ";"),
        DOUBLE_PIPE(Pattern.quote("||"), ESCAPE_DOUBLE_PIPE, "||"),
        COLON(":", ESCAPE_COLON, ":"),
        DASH("-", ESCAPE_DASH, "-"),
        SQR_BRCK_OPEN(Pattern.quote("["), ESCAPE_SQR_BRCK_OPEN, "["),
        SQR_BRCK_CLOSE(Pattern.quote("]"), ESCAPE_SQR_BRCK_CLOSE, "]"),
        BACK_SLASH(Pattern.quote("\\"), ESCAPE_BACK_SLASH, "\\\\"),
        FORWARD_SLASH(Pattern.quote("/"), ESCAPE_FORWARD_SLASH, "/"),

        //Newly Added
        QUESTION_MARK(Pattern.quote("?"), ESCAPE_QUESTION_MARK, "?"),
        ASTERISK(Pattern.quote("*"), ESCAPE_ASTERISK, "*"),
        CURLY_BRCK_OPEN(Pattern.quote("{"), ESCAPE_CURLY_BRCK_OPEN, "{"),
        CURLY_BRCK_CLOSE(Pattern.quote("}"), ESCAPE_CURLY_BRCK_CLOSE, "}"),
        OPEN_BRCK(Pattern.quote("("), ESCAPE_OPEN_BRCK, "("),
        CLS_BRCK(Pattern.quote(")"), ESCAPE_CLS_BRCK, ")"),
        COMMA(Pattern.quote(","), ESCAPE_COMMA, ","),
        AMPERSAND(Pattern.quote("&"), ESCAPE_AMPERSAND, "&"),
        PLUS(Pattern.quote("+"), ESCAPE_PLUS, "+"),
        AT_MRK(Pattern.quote("@"), ESCAPE_AT_MRK, "@"),
        HASH_MRK(Pattern.quote("#"), ESCAPE_HASH_MRK, "#"),
        SINGLE_PIPE(Pattern.quote("|"), ESCAPE_SINGLE_PIPE, "|"),
        TILT_MRK(Pattern.quote("~"), ESCAPE_TILT_MRK, "~"),
        PRCNTG_MRK(Pattern.quote("%"), ESCAPE_PRCNTG_MRK, "%"),
        CAP_MRK(Pattern.quote("^"), ESCAPE_CAP_MRK, "^");


        final private String targetString;
        final private String replacement;
        final private String restoreValue;

        public String targetString() {
            return targetString;
        }

        public String replacement() {
            return replacement;
        }

        public String restoreValue() {
            return restoreValue;
        }

        EscapeToken(String pStr, String pReplacement, String pRestVal) {
            targetString = pStr;
            replacement = pReplacement;
            restoreValue = pRestVal;
        }


        /**
         * Given a passed in string, returns what the replacement should be
         * for escaping purposes. If not replacement is found, the passed in
         * string is returned as is.
         *
         * @param pStr
         * @return
         */
        public static String replacementFromString(String pStr)  {
            for (EscapeToken c: EscapeToken.values()) {
                if (c.targetString.equals(pStr)) {
                    return c.replacement();
                }
            }

            return pStr;
        }
    }



    /**
     * Un-escape any special characters contained in {@param pInStr}.
     * @param pInStr - String to un-escape
     * @return
     */
    public static String unescapeSpecialCharacters(String pInStr) {
        /**
         * Change back any "escaped" (I.e. replaced) strings back to their originals
         */
        for (EscapeUtil.EscapeToken t : EscapeUtil.EscapeToken.values()) {
            pInStr = pInStr.replaceAll(t.replacement(), t.restoreValue());
        }

        return pInStr;
    }


    /**
     * Escape any special characters contained in {@param pInStr}.
     * @param pInStr - String to escape
     * @return
     */
    public static String escapeSpecialCharacters(String pInStr) {
        for (EscapeUtil.EscapeToken t : EscapeUtil.EscapeToken.values()) {
            pInStr = pInStr.replaceAll(t.targetString(), t.replacement());
        }

        return pInStr;
    }


    /**
     * Make this class non-instantiable to public
     */
    private EscapeUtil() {}

}
