package edu.dnatools.utils;

import java.io.File;

/**
 * Created by luke on 6/4/16.
 */
public class SystemUtils {
    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    public static boolean deleteFolder(String path) {
        return deleteDirectory(new File(path));
    }

}
