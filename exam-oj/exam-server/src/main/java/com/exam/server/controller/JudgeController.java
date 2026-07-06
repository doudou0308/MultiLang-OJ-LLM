package com.exam.server.controller;

import com.alibaba.fastjson.JSON;
import com.exam.common.PageDTO;
import com.exam.common.R;
import com.exam.pojo.dto.ProblemAddReq;
import com.exam.pojo.dto.ProblemQueryReq;
import com.exam.pojo.dto.ExecuteCodeReq;
import com.exam.pojo.dto.ProblemSubmitAddReq;
import com.exam.pojo.entity.Problem;
import com.exam.pojo.vo.*;
import com.exam.server.service.JudgeService;
import com.exam.server.service.ProblemService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

    @Resource
    private ProblemService problemService;
    @Resource
    private JudgeService judgeService;

    @PostMapping("/problem")
    public R<Long> addProblem(@RequestBody ProblemAddReq req) {
        ProblemVo vo = problemService.add(req);
        return R.success(vo.getId());
    }

    @PostMapping("/problem/page/vo")
    public R<PageDTO<ProblemVo>> pageVo(@RequestBody ProblemQueryReq req) {
        return R.success(problemService.pageVo(req, true));
    }

    @PostMapping("/problem/page/vo/safe")
    public R<PageDTO<ProblemVo>> pageSafeVo(@RequestBody ProblemQueryReq req) {
        return R.success(problemService.pageVo(req, true));
    }

    @PutMapping("/problem")
    public R<?> updateProblem(@RequestBody Problem problem) {
        problemService.update(problem);
        return R.success();
    }

    @DeleteMapping("/problem/{id}")
    public R<?> deleteProblem(@PathVariable Long id) {
        problemService.delete(id);
        return R.success();
    }

    @GetMapping("/problem/{id}/vo")
    public R<ProblemVo> getVo(@PathVariable Long id) {
        return R.success(problemService.getVo(id, false));
    }

    @GetMapping("/problem/{id}/vo/safe")
    public R<ProblemVo> getSafeVo(@PathVariable Long id) {
        return R.success(problemService.getVo(id, true));
    }

    @GetMapping("/problem/tags")
    public R<List<String>> getTags() {
        return R.success(problemService.getTags());
    }

    @GetMapping("/problem/{id}/{direction}")
    public R<Long> getPrevNext(@PathVariable Long id, @PathVariable String direction) {
        return R.success(problemService.getPrevNextId(id, direction));
    }

    @PostMapping("/run")
    public R<ExecuteCodeRes> run(@RequestBody ExecuteCodeReq req) {
        return R.success(judgeService.run(req.getCode(), req.getLanguage(), req.getInputList()));
    }

    @PostMapping("/submit")
    public R<ProblemSubmitVo> submit(@RequestBody ProblemSubmitAddReq req) {
        return R.success(judgeService.submit(req));
    }

    @PostMapping("/submit/page/vo")
    public R<PageDTO<ProblemSubmitVo>> submitPage(@RequestBody ProblemQueryReq req) {
        return R.success(judgeService.pageSubmits(
                req.getCurrent() != null ? req.getCurrent().intValue() : null,
                req.getSize() != null ? req.getSize().intValue() : null,
                req.getKeyword() != null ? Long.valueOf(req.getKeyword()) : null));
    }

    @GetMapping("/submit/summary")
    public R<SubmitSummaryVo> submitSummary() {
        return R.success(judgeService.getSummary());
    }

    @GetMapping("/submit/checkIn/{date}")
    public R<List<String>> checkIn(@PathVariable String date) {
        return R.success(judgeService.getCheckInDates(date));
    }
}
