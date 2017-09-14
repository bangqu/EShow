package cn.org.eshow.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * URL工具类
 *
 * @author leida
 */
public class UrlUtil {

    private static final Log log = LogFactory.getLog(UrlUtil.class);

    /**
     * 修改Url的某个参数的值。
     *
     * @param url   要替换参数的URL
     * @param key   要替换参数的name
     * @param value 要为该参数替换的值
     * @param isAdd 如果该参数不存在,是否添加
     * @return String
     */
    public static String parseUrl(String url, String key, String value, boolean isAdd) {
        Assert.notNull(url, "参数不能为空");
        Assert.notNull(key, "参数不能为空");
        boolean isExist = false;
        String[] split1 = url.split("\\?");
        String urlLeft = split1[0]; // URI
        String query = split1.length == 2 ? split1[1] : ""; // 参数
        if (split1.length >= 2) {// 如果有参数的话
            query = split1[1];
        } else {// 如果有参数的话
            if (isAdd) {// 如果添加
                return split1[0] + "?" + key + "=" + value;
            } else {// 如果不添加
                return split1[0];
            }
        }
        String[] parseUrl = query.split("&");
        query = "";
        for (int i = 0; i < parseUrl.length; i++) {
            String[] split2 = parseUrl[i].split("=");
            String paramName = split2[0];
            String paramValue = split2.length == 2 ? split2[1] : "";
            if (paramName.equals(key)) {// 如果这个参数的要修改的参数
                isExist = true;
                paramValue = value;
            }
            if (query.equals("")) {
                query = paramName + "=" + paramValue;
            } else {
                query = query + "&" + paramName + "=" + paramValue;
            }
        }
        url = query.equals("") ? urlLeft : urlLeft + "?" + query;
        if (isAdd && !isExist) {// 如果需要添加,并且不存在,就添加
            url = UrlUtil.addParam(url, key, value);
        }
        if (log.isDebugEnabled()) {
            log.debug("url: '" + url + "'");
        }
        return url;
    }

    /**
     * 添加参数
     *
     * @param url
     * @param key
     * @param value
     * @return String
     */
    public static String addParam(String url, String key, String value) {
        Assert.notNull(url, "参数不能为空");
        Assert.notNull(key, "参数不能为空");
        if (url.indexOf("?") < 1) {
            url += "?" + key + "=" + value;
        } else {
            url += "&" + key + "=" + value;
        }
        return url;
    }

    /**
     * @param url
     * @param key
     * @return
     */
    public static String delParam(String url, String key) {
        Assert.notNull(url, "参数不能为空");
        Assert.notNull(key, "参数不能为空");
        String tempUrl = "";
        if (url.indexOf("?") >= 1) {
            String[] tempSplit = url.split("\\?");
            tempUrl = url.split("\\?")[0];
            if (tempSplit.length >= 2) {
                url = url.split("\\?")[1];
            }
        } else {
            return url;
        }
        String[] parseUrl = url.split("&");
        url = "";
        for (int i = 0; i < parseUrl.length; i++) {
            String[] split2 = parseUrl[i].split("=");
            if (split2.length <= 0) {
                continue;
            }
            if (!split2[0].equals(key)) {
                if (split2.length >= 2) {
                    url += split2[0] + "=" + split2[1] + "&";
                } else {
                    url += split2[0] + "=" + "" + "&";
                }
            }

        }
        if ((url.lastIndexOf("&") != -1)
                && (url.lastIndexOf("&") == (url.length() - 1))) {
            url = url.substring(0, url.lastIndexOf("&"));
        }
        if (url.equals("")) {
            return tempUrl;
        } else {
            return tempUrl + "?" + url;
        }
    }

    public static void main(String[] args) {
        // String url = "http://127.o.o.1:8080/vvgood/jsp.jsp";
        // System.out.println(UrlUtil.parseUrl(url, "cc", "cccccc", true));
        // url = "http://127.o.o.1:8080/vvgood/jsp.jsp";
        // System.out.println(UrlUtil.parseUrl(url, "b", "cccccc", false));
        //
        // url = "http://127.o.o.1:8080/vvgood/jsp.jsp?a=c";
        // System.out.println(UrlUtil.delParam(url, "a"));
        // System.out.println(UrlUtil.parseUrl("http://www.baidu.com",
        // "/a/a奇虎.jsp?c=f"));

    }
}
