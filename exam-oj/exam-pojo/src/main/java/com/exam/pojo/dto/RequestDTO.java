package com.exam.pojo.dto;

import lombok.Data;

@Data
public class AccountLoginReq {
    private String email;
    private String password;
}

@Data
public class CodeLoginReq {
    private String email;
    private String captcha;
}

@Data
public class ExecuteCodeReq {
    private String[] inputList;
    private String code;
    private String language;
}

@Data
public class ProblemSubmitAddReq {
    private String language;
    private String code;
    private Long problemId;
}
