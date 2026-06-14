package com.baiye.agentforgeuser.service.impl;

import com.baiye.agentforge.innerservice.InnerUserService;
import com.baiye.agentforge.model.entity.User;
import com.baiye.agentforge.model.vo.UserVO;
import com.baiye.agentforgeuser.service.UserService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * ClassName: InnerUserServiceImpl
 * Package: com.baiye.agentforgeuser.service.impl
 * Description:
 *
 * @Author 白夜
 * @Create 2026/6/14 15:41
 * @Version 1.0
 */
@DubboService
public class InnerUserServiceImpl implements InnerUserService {

    @Resource
    private UserService userService;

    @Override
    public List<User> listByIds(Collection<? extends Serializable> ids) {
        return userService.listByIds(ids);
    }

    @Override
    public User getById(Serializable id) {
        return userService.getById(id);
    }

    @Override
    public UserVO getUserVO(User user) {
        return userService.getUserVO(user);
    }
}

