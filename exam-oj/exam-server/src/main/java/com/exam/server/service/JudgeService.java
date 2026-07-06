package com.exam.server.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.common.PageDTO;
import com.exam.common.SecurityUtil;
import com.exam.pojo.dto.ProblemAddReq;
import com.exam.pojo.dto.ProblemSubmitAddReq;
import com.exam.pojo.entity.Problem;
import com.exam.pojo.entity.ProblemSubmit;
import com.exam.pojo.entity.CheckIn;
import com.exam.pojo.vo.*;
import com.exam.server.mapper.CheckInMapper;
import com.exam.server.mapper.ProblemMapper;
import com.exam.server.mapper.ProblemSubmitMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class JudgeService {

    @Resource
    private ProblemMapper problemMapper;
    @Resource
    private ProblemSubmitMapper submitMapper;
    @Resource
    private CheckInMapper checkInMapper;

    private static final Path WORK_DIR = Path.of(System.getProperty("java.io.tmpdir"), "exam-oj");

    public ExecuteCodeRes run(String code, String language, String[] inputList) {
        ExecuteCodeRes res = new ExecuteCodeRes();
        List<ExecuteResult> results = new ArrayList<>();
        try {
            Files.createDirectories(WORK_DIR);
            for (String input : inputList) {
                ProcessResult pr = compileAndRun(code, language, input);
                ExecuteResult er = new ExecuteResult();
                er.setExitCode(pr.exitCode);
                er.setStdout(pr.stdout);
                er.setStderr(pr.stderr);
                er.setTime(pr.time);
                er.setMemory(pr.memory);
                results.add(er);
            }
            res.setCode(200);
            res.setMsg("ok");
        } catch (Exception e) {
            res.setCode(500);
            res.setMsg(e.getMessage());
        }
        res.setResults(results.toArray(new ExecuteResult[0]));
        return res;
    }

    @Transactional
    public ProblemSubmitVo submit(ProblemSubmitAddReq req) {
        Problem problem = problemMapper.selectById(req.getProblemId());
        if (problem == null) throw new RuntimeException("题目不存在");

        ProblemAddReq.JudgeCase[] cases;
        ProblemAddReq.JudgeConfig config;
        try {
            cases = JSON.parseObject(problem.getJudgeCase(), ProblemAddReq.JudgeCase[].class);
            config = JSON.parseObject(problem.getJudgeConfig(), ProblemAddReq.JudgeConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("题目测试用例解析失败");
        }

        int pass = 0, total = cases.length;
        long maxTime = 0, maxMemory = 0;
        String wrongInput = null, wrongOutput = null, wrongExpected = null;

        for (ProblemAddReq.JudgeCase tc : cases) {
            ProcessResult pr = compileAndRun(req.getCode(), req.getLanguage(), tc.getInput());
            maxTime = Math.max(maxTime, pr.time);
            maxMemory = Math.max(maxMemory, pr.memory);
            if (pr.exitCode != 0) {
                JudgeInfo ji = JudgeInfo.error("Runtime Error: " + pr.stderr);
                return saveSubmit(req, problem, ji, total, pass);
            }
            String actual = pr.stdout.trim().replace("\r\n", "\n");
            String expected = tc.getOutput().trim().replace("\r\n", "\n");
            if (actual.equals(expected)) {
                pass++;
            } else if (wrongInput == null) {
                wrongInput = tc.getInput();
                wrongOutput = actual;
                wrongExpected = expected;
            }
        }

        JudgeInfo ji;
        if (pass == total) {
            ji = JudgeInfo.ok(pass, total, maxTime, maxMemory);
        } else {
            ji = JudgeInfo.wrong(pass, total, wrongInput, wrongOutput, wrongExpected);
        }

        return saveSubmit(req, problem, ji, total, pass);
    }

    private ProblemSubmitVo saveSubmit(ProblemSubmitAddReq req, Problem problem,
            JudgeInfo ji, int total, int pass) {
        ProblemSubmit submit = new ProblemSubmit();
        submit.setProblemId(req.getProblemId());
        submit.setUserId(SecurityUtil.getUserId());
        submit.setLanguage(req.getLanguage());
        submit.setCode(req.getCode());
        int status = (ji == null) ? 6 : ji.getStatus();
        submit.setStatus(status);
        submit.setJudgeInfo(JSON.toJSONString(ji));
        submitMapper.insert(submit);

        problem.setSubmitNum(problem.getSubmitNum() + 1);
        if (pass == total) problem.setAcceptedNum(problem.getAcceptedNum() + 1);
        problemMapper.updateById(problem);

        ProblemSubmitVo vo = new ProblemSubmitVo();
        vo.setId(submit.getId());
        vo.setLanguage(req.getLanguage());
        vo.setCode(req.getCode());
        vo.setJudgeInfo(ji);
        vo.setStatus(status);
        vo.setProblemId(req.getProblemId());
        vo.setUserId(SecurityUtil.getUserId());
        vo.setCreateTime(submit.getCreateTime() != null ? submit.getCreateTime().toString() : null);
        return vo;
    }

    public PageDTO<ProblemSubmitVo> pageSubmits(Integer current, Integer size, Long problemId) {
        Page<ProblemSubmit> page = new Page<>(current != null ? current : 1, size != null ? size : 10);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ProblemSubmit> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(ProblemSubmit::getUserId, SecurityUtil.getUserId());
        if (problemId != null) qw.eq(ProblemSubmit::getProblemId, problemId);
        qw.orderByDesc(ProblemSubmit::getId);
        Page<ProblemSubmit> result = submitMapper.selectPage(page, qw);

        List<ProblemSubmitVo> vos = new ArrayList<>();
        for (ProblemSubmit s : result.getRecords()) {
            ProblemSubmitVo vo = new ProblemSubmitVo();
            vo.setId(s.getId());
            vo.setLanguage(s.getLanguage());
            vo.setCode(s.getCode());
            vo.setJudgeInfo(s.getJudgeInfo() != null
                    ? JSON.parseObject(s.getJudgeInfo(), JudgeInfo.class) : null);
            vo.setStatus(s.getStatus());
            vo.setProblemId(s.getProblemId());
            vo.setUserId(s.getUserId());
            vos.add(vo);
        }
        PageDTO<ProblemSubmitVo> dto = new PageDTO<>();
        dto.setRecords(vos);
        dto.setTotal(result.getTotal());
        return dto;
    }

    public SubmitSummaryVo getSummary() {
        SubmitSummaryVo vo = new SubmitSummaryVo();
        List<ProblemSubmit> submits = submitMapper.selectList(null);
        vo.setSubmitCount(submits.size());
        vo.setPassCount((int) submits.stream().filter(s -> s.getStatus() != null && s.getStatus() == 2).count());
        return vo;
    }

    public List<String> getCheckInDates(String yearMonth) {
        List<CheckIn> list = checkInMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, SecurityUtil.getUserId())
                        .likeRight(CheckIn::getDate, yearMonth));
        List<String> dates = new ArrayList<>();
        for (CheckIn c : list) dates.add(c.getDate().toString());
        return dates;
    }

    public void checkIn() {
        LocalDate today = LocalDate.now();
        Long count = checkInMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckIn>()
                        .eq(CheckIn::getUserId, SecurityUtil.getUserId())
                        .eq(CheckIn::getDate, today));
        if (count == 0) {
            CheckIn c = new CheckIn();
            c.setUserId(SecurityUtil.getUserId());
            c.setDate(today);
            checkInMapper.insert(c);
        }
    }

    static class ProcessResult {
        int exitCode;
        String stdout;
        String stderr;
        long time;
        long memory;
    }

    private ProcessResult compileAndRun(String code, String language, String input) {
        ProcessResult result = new ProcessResult();
        try {
            Files.createDirectories(WORK_DIR);
            long start = System.currentTimeMillis();

            ProcessBuilder pb;
            if ("Java".equals(language)) {
                Path javaFile = WORK_DIR.resolve("Main.java");
                Files.writeString(javaFile, code);
                new ProcessBuilder("javac", javaFile.toAbsolutePath().toString())
                        .directory(WORK_DIR.toFile()).start().waitFor();

                pb = new ProcessBuilder("java", "-cp", WORK_DIR.toAbsolutePath().toString(), "Main");
            } else {
                Path cFile = WORK_DIR.resolve("main.c");
                Files.writeString(cFile, code);
                new ProcessBuilder("gcc", cFile.toAbsolutePath().toString(),
                        "-o", WORK_DIR.resolve("main").toAbsolutePath().toString())
                        .directory(WORK_DIR.toFile()).start().waitFor();

                pb = new ProcessBuilder(WORK_DIR.resolve("main").toAbsolutePath().toString());
            }

            Process process = pb.directory(WORK_DIR.toFile()).start();
            if (input != null) {
                try (BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream()))) {
                    bw.write(input);
                    bw.flush();
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                result.exitCode = -1;
                result.stderr = "Time Limit Exceeded";
                return result;
            }

            result.exitCode = process.exitValue();
            result.stdout = new String(process.getInputStream().readAllBytes());
            result.stderr = new String(process.getErrorStream().readAllBytes());
            result.time = System.currentTimeMillis() - start;
            result.memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            result.memory /= 1024 / 1024;
        } catch (Exception e) {
            result.exitCode = -1;
            result.stderr = e.getMessage() != null ? e.getMessage() : "Compilation Error";
        }
        return result;
    }
}
