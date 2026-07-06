package com.exam.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("problem_submit")
public class ProblemSubmit {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long problemId;
    private Long userId;
    private String language;
    private String code;
    private String judgeInfo;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
