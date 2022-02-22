package com.ligongku.nlpparser.util;

import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;

/**
 * Pack:       com.ligongku.nlpparser.util
 * File:       FileUtil
 * Desc:
 * User:       chuangfengwang
 * CreateTime: 2022-02-22 04:45
 */
public class FileUtil {
    public static void makeFileIfNotExist(String path) {
        if (!(path.startsWith(".") || path.startsWith("/") || path.contains(":"))) {
            // 相对路径, 添加 ./ 前缀
            path = Joiner.on(File.separator).join(".", path);
        }
        File file = new File(path);
        // 检查父目录是否存在
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        // 检查文件是否存在
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
