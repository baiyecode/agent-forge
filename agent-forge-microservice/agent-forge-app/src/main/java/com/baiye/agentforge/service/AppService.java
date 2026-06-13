package com.baiye.agentforge.service;

import com.baiye.agentforge.model.dto.app.AppAddRequest;
import com.baiye.agentforge.model.dto.app.AppQueryRequest;
import com.baiye.agentforge.model.entity.App;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/baiyecode">白夜</a>
 */
public interface AppService extends IService<App> {


    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param loginUser     登录用户
     * @return 应用id
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 获取应用视图对象
     *
     * @param app 应用
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图对象列表
     *
     * @param appList 应用列表
     * @return 应用视图对象列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 获取应用查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 调用门面生成代码
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 部署应用
     * @param appId
     * @param loginUser
     * @return
     */
    String deployApp(Long appId, User loginUser);



    /**
     * 异步生成应用截图并更新封面
     * @param appId
     * @param appUrl
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);
}
