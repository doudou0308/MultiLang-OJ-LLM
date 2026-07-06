package com.exam.pojo.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserVo {
    private Long uid;
    private String username;
    private String email;
    private String[] tags;
    private String signature;
    private Integer sex;
    private String avatar;
    private String userRole;
}
