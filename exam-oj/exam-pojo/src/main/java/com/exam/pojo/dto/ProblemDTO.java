package com.exam.pojo.dto;

import lombok.Data;

@Data
public class ProblemAddReq {
    private String title;
    private String content;
    private String difficulty;
    private String[] tags;
    private String answer;
    private JudgeCase[] judgeCase;
    private JudgeConfig judgeConfig;

    @Data
    public static class JudgeCase {
        private String input;
        private String output;
    }

    @Data
    public static class JudgeConfig {
        private Integer timeLimit;
        private Integer memoryLimit;
    }
}

@Data
public class ProblemQueryReq {
    private Long current;
    private Long size;
    private String[] tags;
    private String status;
    private String difficulty;
    private String keyword;
}
