package com.example.rtspstreaming.controller;

import com.example.rtspstreaming.service.FFmpegService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// import jakarta.servlet.http.HttpServletRequest; // This line will be changed
import javax.servlet.http.HttpServletRequest; // This is the new line

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
public class StreamingController {

    private static final Logger logger = LoggerFactory.getLogger(StreamingController.class);

    @Autowired
    private FFmpegService ffmpegService;

    @Value("${ffmpeg.output.directory}")
    private String hlsOutputDirectory; // FFmpeg输出HLS文件的目录

    @GetMapping("/start_stream")
    public ResponseEntity<String> startStream(@RequestParam String rtsp_url, HttpServletRequest request) {
        String streamId = UUID.randomUUID().toString(); // 为每个流生成唯一的ID
        logger.info("收到启动流的请求，RTSP URL: {}，streamId: {}", rtsp_url, streamId);

        if (ffmpegService.isStreaming(streamId)) {
             // 如果流已在运行，返回现有流的URL
             return ResponseEntity.ok("流已在运行: /hls/" + streamId + "/stream.m3u8");
        }

        try {
            ffmpegService.startStream(rtsp_url, streamId); // 异步启动FFmpeg流处理

            // 构建HLS播放列表URL
            // 基础URL (例如, http://localhost:8080)
            String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
            String playlistUrl = baseUrl + "/hls/" + streamId + "/stream.m3u8"; // 路径与静态资源处理器配置相对应

            // 添加一个小延迟，让FFmpeg有时间创建初始文件
            // 这是一个权宜之计，更健壮的方案是轮询检查文件是否存在并设置超时
            try {
                Thread.sleep(2000); // 2秒延迟, 根据实际情况调整
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("流启动被中断。");
            }

            // 检查 stream.m3u8 文件是否存在
            File m3u8File = new File(hlsOutputDirectory + File.separator + streamId + File.separator + "stream.m3u8");
            if (!m3u8File.exists()) {
                 logger.error("streamId: {} 的m3u8文件在FFmpeg启动后未找到", streamId);
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("流已启动，但M3U8文件未找到。请检查FFmpeg日志。");
            }

            logger.info("streamId {} 的流已启动。播放列表URL: {}", streamId, playlistUrl);
            return ResponseEntity.ok(playlistUrl);
        } catch (IOException e) {
            logger.error("启动RTSP URL {} 的FFmpeg流时发生错误: {}", rtsp_url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("启动流时发生错误: " + e.getMessage());
        }
    }

    @GetMapping("/stop_stream")
    public ResponseEntity<String> stopStream(@RequestParam String streamId) {
        logger.info("收到停止streamId: {} 的流的请求", streamId);
        ffmpegService.stopStream(streamId);
        return ResponseEntity.ok("streamId: " + streamId + " 的流已停止");
    }
}
