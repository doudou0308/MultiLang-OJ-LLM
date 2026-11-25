package io.multilang.oj.http;

import io.multilang.oj.runner.LocalEvaluation.RunnerBusy;
import io.multilang.oj.runner.LocalEvaluation.RunnerUnavailable;
import java.util.NoSuchElementException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    public record ErrorBody(String code, String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorBody invalid(IllegalArgumentException error) {
        return new ErrorBody("INVALID_INPUT", error.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorBody malformed() {
        return new ErrorBody("INVALID_JSON", "请求数据格式或字段不正确");
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorBody missing(NoSuchElementException error) {
        return new ErrorBody("NOT_FOUND", error.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ErrorBody conflict() {
        return new ErrorBody("DUPLICATE_SLUG", "题目标识已存在");
    }

    @ExceptionHandler(RunnerBusy.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    ErrorBody busy() {
        return new ErrorBody("RUNNER_BUSY", "本机判题队列已满，请稍后重试");
    }

    @ExceptionHandler(RunnerUnavailable.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorBody unavailable(RunnerUnavailable error) {
        return new ErrorBody("TOOLCHAIN_UNAVAILABLE", error.getMessage());
    }
}
