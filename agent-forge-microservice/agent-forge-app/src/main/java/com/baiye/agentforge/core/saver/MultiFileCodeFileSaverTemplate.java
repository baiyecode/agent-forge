package com.baiye.agentforge.core.saver;


import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.ai.model.MultiFileCodeResult;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;

/**
 * ClassName: MultiFileCodeFileSaverTemplate
 * Package: com.baiye.agentforge.saver
 * Description: 多文件代码保存器模板
 *
 * @Author 白夜
 * @Create 2026/5/6 16:09
 * @Version 1.0
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    /**
     * 获取代码类型
     * @return
     */
    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    /**
     * 保存文件
     * @param result
     * @param baseDirPath
     */
    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    /**
     * 验证输入参数
     * @param result
     */
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}

