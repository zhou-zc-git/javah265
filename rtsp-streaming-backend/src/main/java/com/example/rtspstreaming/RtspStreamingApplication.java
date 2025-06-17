package com.example.rtspstreaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 启用异步处理，用于FFmpeg进程
public class RtspStreamingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RtspStreamingApplication.class, args);
    }
}
