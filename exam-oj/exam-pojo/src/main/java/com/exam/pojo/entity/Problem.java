package com.exam.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("problem")
public class Problem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String difficulty;
    private String tags;
    private String answer;
    private String judgeCase;
    private String judgeConfig;
    private Integer submitNum;
    private Integer acceptedNum;
    private Integer thumbNum;
    private Integer favourNum;
    private Long userId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
