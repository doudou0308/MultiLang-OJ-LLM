package com.exam.server.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.exam.common.PageDTO;
import com.exam.common.SecurityUtil;
import com.exam.pojo.dto.ProblemAddReq;
import com.exam.pojo.dto.ProblemQueryReq;
import com.exam.pojo.entity.Problem;
import com.exam.pojo.vo.ProblemVo;
import com.exam.server.mapper.ProblemMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProblemService extends ServiceImpl<ProblemMapper, Problem> {

    public ProblemVo add(ProblemAddReq req) {
        Problem p = new Problem();
        p.setTitle(req.getTitle());
        p.setContent(req.getContent());
        p.setDifficulty(req.getDifficulty());
        p.setTags(req.getTags() != null ? JSON.toJSONString(req.getTags()) : null);
        p.setAnswer(req.getAnswer());
        p.setJudgeCase(req.getJudgeCase() != null ? JSON.toJSONString(req.getJudgeCase()) : null);
        p.setJudgeConfig(req.getJudgeConfig() != null ? JSON.toJSONString(req.getJudgeConfig()) : null);
        p.setSubmitNum(0);
        p.setAcceptedNum(0);
        p.setThumbNum(0);
        p.setFavourNum(0);
        p.setUserId(SecurityUtil.getUserId());
        save(p);
        return ProblemVo.from(p, true);
    }

    public PageDTO<ProblemVo> pageVo(ProblemQueryReq req, boolean isSafe) {
        Page<Problem> page = new Page<>(req.getCurrent() != null ? req.getCurrent() : 1,
                req.getSize() != null ? req.getSize() : 10);
        LambdaQueryWrapper<Problem> qw = Wrappers.lambdaQuery();
        if (req.getDifficulty() != null) qw.eq(Problem::getDifficulty, req.getDifficulty());
        if (req.getKeyword() != null) qw.like(Problem::getTitle, req.getKeyword());
        qw.orderByDesc(Problem::getId);
        Page<Problem> result = page(page, qw);

        List<ProblemVo> vos = new ArrayList<>();
        for (Problem p : result.getRecords()) {
            vos.add(ProblemVo.from(p, !isSafe));
        }
        PageDTO<ProblemVo> dto = new PageDTO<>();
        dto.setRecords(vos);
        dto.setTotal(result.getTotal());
        dto.setSize(result.getSize());
        dto.setCurrent(result.getCurrent());
        dto.setPages(result.getPages());
        return dto;
    }

    public ProblemVo getVo(Long id, boolean isSafe) {
        Problem p = getById(id);
        return p == null ? null : ProblemVo.from(p, !isSafe);
    }

    public void update(Problem problem) {
        updateById(problem);
    }

    public void delete(Long id) {
        removeById(id);
    }

    public List<String> getTags() {
        List<Problem> list = list();
        Set<String> tagSet = new LinkedHashSet<>();
        for (Problem p : list) {
            if (p.getTags() != null) {
                String[] tags = JSON.parseObject(p.getTags(), String[].class);
                Collections.addAll(tagSet, tags);
            }
        }
        return new ArrayList<>(tagSet);
    }

    public Long getPrevNextId(Long id, String direction) {
        List<Problem> list = list(Wrappers.<Problem>lambdaQuery()
                .orderByAsc(Problem::getId));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                if ("prev".equals(direction) && i > 0) return list.get(i - 1).getId();
                if ("next".equals(direction) && i < list.size() - 1) return list.get(i + 1).getId();
                return -1L;
            }
        }
        return -1L;
    }
}
