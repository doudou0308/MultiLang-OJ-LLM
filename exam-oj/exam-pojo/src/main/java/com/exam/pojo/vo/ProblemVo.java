package com.exam.pojo.vo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.exam.pojo.dto.ProblemAddReq.JudgeCase;
import com.exam.pojo.dto.ProblemAddReq.JudgeConfig;
import com.exam.pojo.entity.Problem;
import lombok.Data;

@Data
public class ProblemVo {
    private Long id;
    private String title;
    private String content;
    private String difficulty;
    private String[] tags;
    private String answer;
    private JudgeCase[] judgeCase;
    private JudgeConfig judgeConfig;
    private Integer submitNum;
    private Integer acceptedNum;
    private Integer thumbNum;
    private Integer favourNum;
    private Long userId;
    private String createTime;
    private String updateTime;

    public static ProblemVo from(Problem p, boolean includeAnswer) {
        ProblemVo vo = new ProblemVo();
        vo.id = p.getId();
        vo.title = p.getTitle();
        vo.content = p.getContent();
        vo.difficulty = p.getDifficulty();
        vo.tags = p.getTags() != null ? JSON.parseObject(p.getTags(), String[].class) : new String[0];
        if (includeAnswer) vo.answer = p.getAnswer();
        vo.judgeCase = p.getJudgeCase() != null
                ? JSON.parseObject(p.getJudgeCase(), JudgeCase[].class) : new JudgeCase[0];
        vo.judgeConfig = p.getJudgeConfig() != null
                ? JSON.parseObject(p.getJudgeConfig(), JudgeConfig.class) : null;
        vo.submitNum = p.getSubmitNum();
        vo.acceptedNum = p.getAcceptedNum();
        vo.thumbNum = p.getThumbNum();
        vo.favourNum = p.getFavourNum();
        vo.userId = p.getUserId();
        vo.createTime = p.getCreateTime() != null ? p.getCreateTime().toString() : null;
        vo.updateTime = p.getUpdateTime() != null ? p.getUpdateTime().toString() : null;
        return vo;
    }
}
