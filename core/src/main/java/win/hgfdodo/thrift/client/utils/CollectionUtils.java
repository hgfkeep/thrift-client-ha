package win.hgfdodo.thrift.client.utils;

import java.util.Collection;

/**
 * Author: guangfuhe<br/>
 * Date: 2020/6/3<br/>
 * Time: 6:43 下午<br/>
 * 集合类工具函数
 */
public class CollectionUtils {
    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }

}
