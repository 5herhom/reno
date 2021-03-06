package cn.com.sherhom.reno.common.utils;

import cn.com.sherhom.reno.common.exception.RenoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * @author Sherhom
 * @date 2020/9/2 20:12
 */
@Slf4j
public class Asset {
    public static void notNull(Object o,String msg) {
        if(o==null)
            throwException(msg);
    }
    public static void notNull(Object o,String pattern,Object... args) {
        notNull(o, MessageFormat.format(pattern,args));
    }
    public static void notEmpty(Collection<?> collection , String msg) {
        if(CollectionUtils.isEmpty(collection))
            throwException(msg);
    }
    public static void notEmpty(Collection<?> o,String pattern,Object... args) {
        notEmpty(o, MessageFormat.format(pattern,args));
    }
    public static void isNotBlank(String s,String msg) {
        if(!StringUtils.isNotBlank(s))
            throwException(msg);
    }
    public static void isNotBlank(String o,String pattern,Object... args) {
        isNotBlank(o, MessageFormat.format(pattern,args));
    }
    public static void isTrue(boolean flag,String msg){
        if(!flag)
            throwException(msg);
    }
    public static void isTrue(boolean flag,String pattern,Object... args) {
        isTrue(flag, MessageFormat.format(pattern,args));
    }
    public static void isFalse(boolean flag,String msg){
        if(flag)
            throwException(msg);
    }
    public static void isFalse(boolean flag,String pattern,Object... args) {
        isFalse(flag, MessageFormat.format(pattern,args));
    }
    public static void isEqual(Object o1,Object o2,String msg){
        if(o1==null&&o2==null)
            return;
        if((o1==null)||!o1.equals(o2))
            throwException(msg);
    }
    public static void isEqual(Object o1,Object o2,String pattern,Object... args) {
        isEqual(o1,o2, MessageFormat.format(pattern,args));
    }
    public static void throwException(String msg){
        log.error(msg);
        throw new RenoException(msg);
    }
}
