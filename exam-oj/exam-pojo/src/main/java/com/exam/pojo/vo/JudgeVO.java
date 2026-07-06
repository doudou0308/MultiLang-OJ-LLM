package com.exam.pojo.vo;

import lombok.Data;

@Data
public class JudgeInfo {
    private int status;
    private String msg;
    private int pass;
    private int total;
    private long memory;
    private long time;
    private String input;
    private String output;
    private String expectedOutput;

    public static JudgeInfo ok(int pass, int total, long time, long memory) {
        JudgeInfo info = new JudgeInfo();
        info.status = 2;
        info.msg = "Accepted";
        info.pass = pass;
        info.total = total;
        info.time = time;
        info.memory = memory;
        return info;
    }

    public static JudgeInfo wrong(int pass, int total, String input, String output, String expected) {
        JudgeInfo info = new JudgeInfo();
        info.status = 3;
        info.msg = "Wrong Answer";
        info.pass = pass;
        info.total = total;
        info.input = input;
        info.output = output;
        info.expectedOutput = expected;
        return info;
    }

    public static JudgeInfo error(String msg) {
        JudgeInfo info = new JudgeInfo();
        info.status = 6;
        info.msg = msg;
        return info;
    }
}

@Data
public class ProblemSubmitVo {
    private Long id;
    private String language;
    private String code;
    private JudgeInfo judgeInfo;
    private int status;
    private Long problemId;
    private Long userId;
    private String createTime;
    private String updateTime;
}

@Data
public class SubmitSummaryVo {
    private int easyPass;
    private int easyTotal;
    private int mediumPass;
    private int mediumTotal;
    private int hardPass;
    private int hardTotal;
    private int submitCount;
    private int passCount;
}

@Data
public class ExecuteResult {
    private int exitCode;
    private String stdout;
    private String stderr;
    private long time;
    private long memory;
}

@Data
public class ExecuteCodeRes {
    private int code;
    private String msg;
    private ExecuteResult[] results;
}
