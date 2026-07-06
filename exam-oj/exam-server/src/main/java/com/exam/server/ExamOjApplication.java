package com.exam.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.exam")
@MapperScan("com.exam.server.mapper")
public class ExamOjApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamOjApplication.class, args);
    }
}
