package com.exam.server.controller;

import com.exam.common.R;
import com.exam.common.SecurityUtil;
import com.exam.pojo.dto.AccountLoginReq;
import com.exam.pojo.vo.UserVo;
import com.exam.server.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody AccountLoginReq req) {
        return R.success(userService.login(req));
    }

    @PostMapping("/loginByCode")
    public R<Map<String, Object>> loginByCode(@RequestBody Map<String, String> body) {
        return R.error("暂不支持验证码登录，请使用邮箱密码登录");
    }

    @GetMapping("/email/sendCode")
    public R<?> sendCode(@RequestParam String email) {
        return R.error("暂不支持邮件验证码");
    }

    @GetMapping("/current")
    public R<UserVo> current() {
        Long userId = SecurityUtil.getUserId();
        if (userId == null) return R.error(401, "未登录");
        UserVo vo = userService.getCurrentUser(userId);
        return vo != null ? R.success(vo) : R.error(401, "用户不存在");
    }

    @GetMapping("/aksk")
    public R<Map<String, String>> aksk() {
        return R.success(Map.of("accessKey", "demo-ak", "secretKey", "demo-sk"));
    }

    @PostMapping("/logout")
    public R<?> logout() {
        return R.success();
    }
}
