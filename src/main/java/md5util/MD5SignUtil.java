package md5util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

public class MD5SignUtil {

    /**
     * @Param object:需要MD5签名的对象
     * @Param secretKey:签名秘钥
     * 签名原理：对于用含有@Param的对象属性，将按照字典序，拼成 a=bbb&c=ddd&.....&kay=secretKey的字符串，然后对其做MD5签名。
     *         如果同时含有@NoSign，该属性将不参与签名
     * */
    public static String sign(Object object, String secretKey) throws Exception {
        if(object == null) {
            throw new NullPointerException("签名内容不能为空");
        }

        return signature(object, "&", secretKey);
    }

    private static String signature(Object object, String separator, String secretKey) throws Exception {
        String result = getSortedString(object, separator, secretKey);
        System.out.println("Origin string:" + result);
        return result != null? content2Sign(result): null;
    }

    private static String getSortedString(Object obj, String concatChar, String key) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        Map<String, Object> map = splitFieldToOrderedMap(obj);

        List<Map.Entry<String, Object>> infoIds = new ArrayList(map.entrySet());
        Collections.sort(infoIds, new Comparator<Map.Entry<String, Object>>() {
            public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                return (o1.getKey()).toString().compareTo(o2.getKey());
            }
        });
        Iterator it = infoIds.iterator();

        while(it.hasNext()) {
            Map.Entry<String, Object> infoId = (Map.Entry)it.next();
            if(StringUtils.isNotEmpty(infoId.getKey())) {
                stringBuilder.append((infoId.getKey()).toString()).append("=").append(infoId.getValue()).append(concatChar);
            }
        }

        stringBuilder.append("key").append("=").append(key);
        return stringBuilder.substring(0, stringBuilder.toString().length());


    }

    private static Map<String, Object> splitFieldToOrderedMap(Object obj) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<String, Object>();
        Field[] fields = getAllFields(obj.getClass());

        for(int index = 0; index < fields.length; ++index) {
            Field field = fields[index];
            field.setAccessible(true);
            if(!field.isAnnotationPresent(Param.class) || field.isAnnotationPresent(NoSign.class)) {
                continue;
            }

            Object fieldObj = field.get(obj);
            Param annoParam = field.getAnnotation(Param.class);
            String fieldName = annoParam.value();

            if(fieldObj != null && !StringUtils.EMPTY.equals(fieldObj) && !fieldName.equals(StringUtils.EMPTY) ) {
                map.put(fieldName, fieldObj);
            }
        }
        return map;
    }

    private static String content2Sign(String content) {
        return DigestUtils.md5Hex(content);
    }

    private static Field[] getAllFields(Class<?> clazz) {
        if(clazz == null) {
            return null;
        }

        Field[] declaredFields = clazz.getDeclaredFields();
        Class<?> parent = clazz.getSuperclass();
        return (Field[])ArrayUtils.addAll(declaredFields, getAllFields(parent));
    }
}
