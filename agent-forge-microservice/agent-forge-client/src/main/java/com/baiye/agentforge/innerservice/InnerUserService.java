package com.baiye.agentforge.innerservice;

import com.baiye.agentforge.exception.BusinessException;
import com.baiye.agentforge.exception.ErrorCode;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.baiye.agentforge.constant.UserConstant.USER_LOGIN_STATE;

/**
 * ClassName: InnerUserService
 * Package: com.baiye.agentforge.innerservice
 * Description: 内部使用的用户服务
 *
 * @Author 白夜
 * @Create 2026/6/12 20:29
 * @Version 1.0
 */
public interface InnerUserService {

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);

    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
}

