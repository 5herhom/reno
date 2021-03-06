package cn.com.sherhom.reno.common.utils;

import lombok.extern.slf4j.Slf4j;
import sun.rmi.runtime.Log;

import java.io.File;
import java.io.IOException;

/**
 * @author Sherhom
 * @date 2020/9/8 20:13
 */
@Slf4j
public class FileUtil {
    public static File openAndCreateFile(String path){
        File file=new File(path);

        if(file.isDirectory())
            return null;

        File parent=file.getParentFile();
        if(!parent.exists()){
            parent.mkdirs();
        }
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                LogUtil.printStackTrace(e);
                return null;
            }
        }
        return file;
    }
    public static String getPathAndFile(String path,String file){
        return path==null?null:
                path.endsWith("/")||path.endsWith("\\")?path+file:path+"/"+file;
    }
}
