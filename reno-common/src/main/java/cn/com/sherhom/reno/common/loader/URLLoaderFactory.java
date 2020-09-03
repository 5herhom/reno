package cn.com.sherhom.reno.common.loader;

import static cn.com.sherhom.reno.common.contants.ProtocolType.CLASSPATH_PROTOCOL;
import static cn.com.sherhom.reno.common.contants.ProtocolType.FILE_PROTOCOL;

public class URLLoaderFactory {
    public static URLLoader getInstance(String protocol,String path){
        switch (protocol.toLowerCase()){
            case CLASSPATH_PROTOCOL:return new ClasspathLoader(path);
            case FILE_PROTOCOL:return new FileLoader(path);
            default:return  new ClasspathLoader(path);
        }
    }
}
