package com.baiye.agentforge.controller;

import com.baiye.agentforge.common.BaseResponse;
import com.baiye.agentforge.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ClassName: HealthController
 * Package: com.baiye.agentforge.controller
 * Description:
 *
 * @Author 白夜
 * @Create 2026/3/20 14:05
 * @Version 1.0
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping("/")
    public BaseResponse<String> healthCheck(){
        return ResultUtils.success("ok");
    }
}
