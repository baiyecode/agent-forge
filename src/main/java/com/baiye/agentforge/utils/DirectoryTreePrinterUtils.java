package com.baiye.agentforge.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;


/**
 * ClassName: DirectoryTreePrinterUtils
 * Package: com.baiye.agentforge.utils
 * Description: 工具类，用于生成目录树的文本表示
 *
 * @Author 白夜
 * @Create 2026/5/23 13:57
 * @Version 1.0
 */
public class DirectoryTreePrinterUtils {


    /**
     * 目录树结构
     *
     * @param directory  要展示的目录路径
     * @param prefix     缩进前缀，用于绘制层级连线（如 "│ " 或 " "）
     * @param isLast     当前目录节点是否是父节点中的最后一个子节点，决定使用 └── 还是 ├──
     * @param fileFilter 文件/目录过滤器，可传入 null 表示不过滤；用于筛选要显示的子项
     */
    public static String directoryTree(String directory, String prefix, boolean isLast, FileFilter fileFilter) {
        StringBuilder sb = new StringBuilder();
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return dir.getName() + " 目录不存在或不是有效目录";
        }
        File[] files = dir.listFiles(fileFilter);// 列出目录下经过过滤的子文件和子目录。
//        File[] files = dir.listFiles();
        if (files == null) return "";

        // 打印当前目录或文件
//        System.out.println(prefix + (isLast ? "└── " : "├── ") + dir.getName());
        //prefix：继承自父级的前缀，包含之前所有层级的缩进和连线。
        sb.append(prefix + (isLast ? "└── " : "├── ") + dir.getName() + "\n");

        // 排序：先目录后文件，按名称排序
        java.util.Arrays.sort(files, Comparator
                // 按 !f.isDirectory() 排序：目录为 false，文件为 true，因为 false < true，所以目录排在文件前面。
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(File::getName));//同级内按名称自然顺序（File.getName()）排序，即字典序。

        for (int i = 0; i < files.length; i++) {
            boolean last = (i == files.length - 1);
            String newPrefix = prefix + (isLast ? "    " : "│   ");
            if (files[i].isDirectory()) {
                // 递归处理子目录
                sb.append(directoryTree(files[i].getPath(), newPrefix, last, fileFilter));
            } else {
                // 处理文件
                sb.append(newPrefix + (last ? "└── " : "├── ") + files[i].getName());
            }
            if (i != files.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    //指定根目录是否为最后一个节点
    //通常根目录的 isLast 设为 true 会让根显示为 └──，否则显示 ├──。
    public static String directoryTree(String directory, boolean isLast) {
        return directoryTree(directory, "", isLast, null);
    }

    //只指定过滤器
    public static String directoryTree(String directory, FileFilter fileFilter) {
        return directoryTree(directory, "", false, fileFilter);
    }

    //全部默认：前缀空，isLast = false，无过滤器。适合打印完整目录树。
    public static String directoryTree(String directory) {
        return directoryTree(directory, "", false, null);
    }

}

