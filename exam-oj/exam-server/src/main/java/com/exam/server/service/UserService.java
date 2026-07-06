package com.exam.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.exam.common.BusinessException;
import com.exam.common.JwtUtil;
import com.exam.pojo.dto.AccountLoginReq;
import com.exam.pojo.entity.User;
import com.exam.pojo.vo.UserVo;
import com.exam.server.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    public Map<String, Object> login(AccountLoginReq req) {
        LambdaQueryWrapper<User> qw = Wrappers.lambdaQuery();
        qw.eq(User::getEmail, req.getEmail());
        User user = userMapper.selectOne(qw);
        if (user == null) throw new BusinessException("用户不存在");
        if ("ban".equals(user.getRole())) throw new BusinessException("账号已被禁用");
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        String token = JwtUtil.generateToken(user.getId(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toVo(user));
        return result;
    }

    public UserVo getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        return user == null ? null : toVo(user);
    }

    private UserVo toVo(User u) {
        UserVo vo = new UserVo();
        vo.setUid(u.getId());
        vo.setUsername(u.getUsername());
        vo.setEmail(u.getEmail());
        vo.setAvatar(u.getAvatar());
        vo.setSignature(u.getSignature());
        vo.setSex(u.getSex());
        vo.setUserRole(u.getRole());
        return vo;
    }
}
