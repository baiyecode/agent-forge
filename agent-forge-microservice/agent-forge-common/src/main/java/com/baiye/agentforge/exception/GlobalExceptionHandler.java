package com.baiye.agentforge.exception;

import cn.hutool.json.JSONUtil;
import com.baiye.agentforge.common.BaseResponse;
import com.baiye.agentforge.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Map;

/**
 * ClassName: GlobalExceptionHandler
 * Package: com.baiye.agentforge.exception
 * Description: 全局异常处理器
 *
 * @Author 白夜
 * @Create 2026/3/20 14:22
 * @Version 1.0
 */
@Hidden //在生成 API 文档时隐藏这个类或方法
@RestControllerAdvice //全局拦截控制器层抛出的异常
@Slf4j
public class GlobalExceptionHandler {

    //处理业务异常
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(e.getCode(), e.getMessage())) {
            //判断当前请求是不是 SSE 请求。
            //如果是，则直接通过 HttpServletResponse 写出 SSE 格式的错误事件流，并返回 true。
            //这时该方法返回 null（Spring 会将 null 视为“已处理完响应，无需再写任何东西”），
            //因为数据已经通过 response.getWriter() 发送给客户端了。
            return null;
        }
        // 对于普通请求，返回标准 JSON 响应
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    //处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        // 尝试处理 SSE 请求
        if (handleSseError(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误")) {
            return null;
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    /**
     * 处理SSE请求的错误响应
     *
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return true表示是SSE请求并已处理，false表示不是SSE请求
     */
    private boolean handleSseError(int errorCode, String errorMessage) {
        //获取当前请求上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        //说明当前不在 Web 请求线程中（例如在异步线程里调用），直接返回 false，让调用方走 JSON 响应逻辑。
        if (attributes == null) {
            return false;
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        // 判断是否是SSE请求（通过Accept头或URL路径）
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        if ((accept != null && accept.contains("text/event-stream")) ||
                uri.contains("/chat/gen/code")) {
            try {
                // 设置SSE响应头
                //告诉客户端：“我返回的不是普通 HTML 或 JSON，而是符合 SSE 规范的事件流”。
                response.setContentType("text/event-stream");
                response.setCharacterEncoding("UTF-8");//SSE 数据中的文本统一使用 UTF-8 编码，避免中文乱码。
                //关键配置。SSE 是实时推送，如果浏览器或中间代理缓存了响应，客户端就会拿到旧数据，失去实时性。
                //no-cache 并不是“不缓存”，而是每次使用缓存前必须先到服务器验证，这能防止中间代理（如 Nginx）缓存 SSE 流。
                response.setHeader("Cache-Control", "no-cache");
                //告诉客户端和中间代理：这个 TCP 连接会保持长时间打开，不要在处理完第一个响应后就关闭。
                //虽然 HTTP/1.1 默认就是 keep-alive，但显式设置可以避免某些老旧代理的干扰，也更清晰表明长连接意图。
                response.setHeader("Connection", "keep-alive");
                // 构造错误消息的SSE格式
                Map<String, Object> errorData = Map.of(
                        "error", true,
                        "code", errorCode,
                        "message", errorMessage
                );
                String errorJson = JSONUtil.toJsonStr(errorData);
                // 发送业务错误事件（避免与标准error事件冲突）
                //自定义事件名称，表示这是一个业务错误。避免使用标准的 error 事件，
                //因为浏览器的 EventSource 在收到 error 事件时会触发连接重试，
                //可能导致意外行为。使用 business-error 让前端可以区分正常数据和业务异常，并做相应处理。
                String sseData = "event: business-error\ndata: " + errorJson + "\n\n";
                response.getWriter().write(sseData);
                response.getWriter().flush();//确保数据立即发送给客户端，而不是留在缓冲区。
                //发送一个自定义的结束事件。
                //因为 SSE 没有官方“结束信号”（除了连接关闭），所以通常约定一个 done 事件，
                //告诉客户端：“数据流完毕，不会再有了，可以关闭连接并处理结果”。
                //这里即使在错误情况下也发送，保证前端能正常退出等待状态。
                response.getWriter().write("event: done\ndata: {}\n\n");
                response.getWriter().flush();//确保数据立即发送给客户端，而不是留在缓冲区。
                // 表示已处理SSE请求
                return true;
            } catch (IOException ioException) {
                log.error("Failed to write SSE error response", ioException);
                // 即使写入失败，也表示这是SSE请求
                return true;
            }
        }
        return false;
    }
}


