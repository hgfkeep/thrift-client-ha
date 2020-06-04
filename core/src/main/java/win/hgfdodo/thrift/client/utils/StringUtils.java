package win.hgfdodo.thrift.client.utils;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/3<br/>
 * Time: 5:16 下午<br/>
 * 处理字符串的工具类
 */
public class StringUtils {

    /**
     * 判断字符串是否为空（`null` 或者""）
     * @param str
     * @return true: 输入字符串为空；false: 输入字符串不为空， 具有有效字符
     */
    public static boolean isEmpty(String str) {
        return !isNotEmpty(str);
    }

    /**
     * 判断字符串是否不为空
     *
     * @param str
     * @return
     */
    public static boolean isNotEmpty(String str) {
        return str != null && str.length() > 0;
    }

}
