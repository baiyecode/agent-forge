package com.baiye.agentforge.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baiye.agentforge.constant.UserConstant;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.exception.ThrowUtils;
import com.baiye.agentforge.mapper.ChatHistoryMapper;
import com.baiye.agentforge.model.dto.chathistory.ChatHistoryQueryRequest;
import com.baiye.agentforge.model.entity.App;
import com.baiye.agentforge.model.entity.ChatHistory;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.model.enums.ChatHistoryMessageTypeEnum;
import com.baiye.agentforge.service.AppService;
import com.baiye.agentforge.service.ChatHistoryService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/baiyecode">白夜</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {


    @Resource
    @Lazy
    private AppService appService;


    /**
     * 获取对话历史查询条件
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        //空值保护
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long appId = chatHistoryQueryRequest.getAppId();
        Long userId = chatHistoryQueryRequest.getUserId();
        //用于游标分页
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        //动态排序
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("appId", appId)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        //lt 表示 小于（<），这里条件是 createTime < lastCreateTime。
        //这是一种典型的游标分页（或键集分页）方式：
        //首次请求时不传 lastCreateTime，加载最新的一批数据。
        //之后前端把当前页最后一条数据的 createTime 作为 lastCreateTime 传给后端，后端就用它查出更早的数据。
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        //动态排序：如果前端传递了 sortField（排序字段名）和 sortOrder（ascend 或 descend），则按指定字段和方向排序。
        //orderBy(sortField, true) → 升序（ASC）
        //orderBy(sortField, false) → 降序（DESC）
        //此处 "ascend".equals(sortOrder) 结果为 true 时升序，否则降序。
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }

    /**
     * 分页获取应用对话历史
     *
     * @param appId          应用id
     * @param pageSize       每页大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser      登录用户
     * @return
     */
    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                                      LocalDateTime lastCreateTime,
                                                      User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 验证权限：只有应用创建者和管理员可以查看
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        boolean isCreator = app.getUserId().equals(loginUser.getId());
        ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该应用的对话历史");
        // 构建查询条件
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setLastCreateTime(lastCreateTime);
        QueryWrapper queryWrapper = this.getQueryWrapper(queryRequest);
        // 查询数据
        //为什么页码固定为 1？因为这里采用了游标分页，而不是传统页码分页。
        //游标分页通过 createTime < lastCreateTime 来获取“下一页”数据，每次查询实际上都是“第一页”，
        //只是数据起点在变。前端只需要传递上一页最后一条记录的 createTime 即可继续加载更早的记录，
        //而不需要知道当前在第几页。
        return this.page(Page.of(1, pageSize), queryWrapper);
    }


    /**
     * 根据应用id删除对话历史
     *
     * @param appId 应用id
     * @return 是否删除成功
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }


    /**
     * 添加对话消息历史
     *
     * @param appId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    @Override
    public boolean addChatMessage(Long appId, String message, String messageType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "不支持的消息类型: " + messageType);
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistory);// 保存到数据库
    }


    /**
     * 加载对话历史到记忆
     *
     * @param appId      应用id
     * @param chatMemory 对话记忆
     * @param maxCount   最大数量
     * @return 加载数量
     */
    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);//执行数据库查询
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            //因为对话是有因果关系的：
            //必须先有“用户问什么”，才有“AI回答什么”。
            //记忆体里消息的顺序必须与实际发生的顺序一致，模型才能正确理解上下文。
            //如果顺序是乱的（比如最新的在最前面），模型就可能把后来的回复当成前面问题的前提，产生理解错乱。
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;//记录成功加载的条数，方便追踪上下文窗口大小。
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }


}

