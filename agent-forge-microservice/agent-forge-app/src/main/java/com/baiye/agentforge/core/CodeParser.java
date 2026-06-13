package com.baiye.agentforge.core;

import com.baiye.agentforge.ai.model.HtmlCodeResult;
import com.baiye.agentforge.ai.model.MultiFileCodeResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ClassName: CodeParser
 * Package: com.baiye.agentforge.core
 * Description: 代码解析器,提供静态方法解析不同类型的代码内容
 *
 * @Author 白夜
 * @Create 2026/5/6 15:09
 * @Version 1.0
 */
@Deprecated // 表示该类已经过时，不建议使用
public class CodeParser {


    /**
     * Pattern.compile 方法用于编译一个正则表达式，并返回一个 Pattern 对象。
     * 返回 Pattern 对象：这个对象就是“编译好的规则”，可以多次用来创建 Matcher 去匹配不同的输入字符串，无需每次重新解析正则。
     * ```html：匹配字符串字面量 ```html。
     * \\s*：匹配任意个空白字符（空格、制表符等），使 ```html 和换行之间可以存在空格。
     * \\n：要求一个换行符（LF）。这表示代码块的语言标记后必须换行再开始正文。
     * ([\\s\\S]*?)：匹配任意字符（包括换行符），非贪婪模式，直到遇到下一个 ```。
     * 这里的 ([\\s\\S]*?) 是捕获组，[\\s\\S]* 表示匹配任意字符（包括换行）任意次，后面的 ? 让它变成非贪婪。
     * 使用非贪婪量词 ([\\s\\S]*?)它会从 \n 之后开始，每匹配一个字符就检查后面的 ``` 是否出现，一旦找到第一个 ``` 就立即停止。
     * ```：匹配代码块结束标记。
     * Pattern.CASE_INSENSITIVE：表示忽略大小写。
     * (?:js|javascript)：非捕获分组，匹配 js 或 javascript，不区分大小写。
     * 捕获组 和 非捕获组 是正则表达式里两种括号的功能区分，关键差别在于：是否保存匹配到的内容，以供后续提取或引用。
     * 捕获组 (...)
     * 作用：把括号内匹配到的内容单独保存起来，之后可以通过编号（如 group(1)）或反向引用（如 \1）来提取或引用。
     * 非捕获组 (?:...)
     * 作用：括号仍然起到分组和优先级控制的作用，但不保存匹配的内容，不占用组号。
     */

    // 代码块正则表达式
    private static final Pattern HTML_CODE_PATTERN = Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern CSS_CODE_PATTERN = Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
    private static final Pattern JS_CODE_PATTERN = Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    /**
     * 解析 HTML 单文件代码
     */
    public static HtmlCodeResult parseHtmlCode(String codeContent) {
        HtmlCodeResult result = new HtmlCodeResult();
        // 提取 HTML 代码
        String htmlCode = extractHtmlCode(codeContent);
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            //String.trim() 方法会返回一个去掉首尾所有空白字符（空格、制表符、换行等）的新字符串。
            //.isEmpty(),判断字符串长度是否为 0。
            // !htmlCode.trim().isEmpty() 的意思是：去除首尾空白后，字符串长度不为 0
            result.setHtmlCode(htmlCode.trim());
        } else {
            // 如果没有找到代码块，将整个内容作为HTML
            result.setHtmlCode(codeContent.trim());
        }
        return result;
    }

    /**
     * 解析多文件代码（HTML + CSS + JS）
     */
    public static MultiFileCodeResult parseMultiFileCode(String codeContent) {
        MultiFileCodeResult result = new MultiFileCodeResult();
        // 提取各类代码
        String htmlCode = extractCodeByPattern(codeContent, HTML_CODE_PATTERN);
        String cssCode = extractCodeByPattern(codeContent, CSS_CODE_PATTERN);
        String jsCode = extractCodeByPattern(codeContent, JS_CODE_PATTERN);
        // 设置HTML代码
        if (htmlCode != null && !htmlCode.trim().isEmpty()) {
            result.setHtmlCode(htmlCode.trim());
        }
        // 设置CSS代码
        if (cssCode != null && !cssCode.trim().isEmpty()) {
            result.setCssCode(cssCode.trim());
        }
        // 设置JS代码
        if (jsCode != null && !jsCode.trim().isEmpty()) {
            result.setJsCode(jsCode.trim());
        }
        return result;
    }

    /**
     * 提取HTML代码内容
     *
     * @param content 原始内容
     * @return HTML代码
     */
    private static String extractHtmlCode(String content) {
        // Pattern.matcher(content) 生成一个 Matcher，它可以将这个模式应用到传入的 content 字符串上，执行查找、匹配等操作。
        // 此时 matcher 已经内置了正则引擎和待搜索的文本，但尚未开始扫描。
        Matcher matcher = HTML_CODE_PATTERN.matcher(content);
        //find() 方法尝试在 content 中从头开始（或从上一次匹配结束位置）寻找下一个匹配该正则的子串。
        //第一次调用时，它会扫描整个字符串，找到第一个满足模式的子串。
        //如果找到，返回 true，同时 Matcher 内部会记录本次匹配的起始和结束位置；若整个字符串都没有符合模式的子串，返回 false。
        if (matcher.find()) {
            return matcher.group(1);
        }
        //group(1) 方法返回第一个捕获组匹配到的内容([\\s\\S]*?)，即 HTML 代码块。
        //group(0) 或 group()：返回整个匹配的文本，即整段 ```html ... ```（包含标记和换行等）。
        return null;
    }

    /**
     * 根据正则模式提取代码
     *
     * @param content 原始内容
     * @param pattern 正则模式
     * @return 提取的代码
     */
    private static String extractCodeByPattern(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}

