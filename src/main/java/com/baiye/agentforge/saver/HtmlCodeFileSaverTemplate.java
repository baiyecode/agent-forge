package com.baiye.agentforge.saver;

import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.ai.model.HtmlCodeResult;
import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.enums.CodeGenTypeEnum;

/**
 * ClassName: HtmlCodeFileSaverTemplate
 * Package: com.baiye.agentforge.saver
 * Description: HTML代码文件保存器模板
 *
 * @Author 白夜
 * @Create 2026/5/6 15:54
 * @Version 1.0
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}

