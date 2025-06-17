package com.example.rtspstreaming.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FFmpegService {

    private static final Logger logger = LoggerFactory.getLogger(FFmpegService.class);
    // 使用ConcurrentHashMap来存储活动流ID及其对应的Process对象，保证线程安全
    private final Map<String, Process> activeStreams = new ConcurrentHashMap<>();

    @Value("${ffmpeg.output.directory}")
    private String hlsOutputDirectory; // 从application.properties注入HLS输出目录

    @Async("threadPoolTaskExecutor") // 指定使用配置的线程池执行异步任务
    public void startStream(String rtspUrl, String streamId) throws IOException {
        // 确保此流的输出目录存在
        File streamOutputDirectory = new File(hlsOutputDirectory + File.separator + streamId);
        if (!streamOutputDirectory.exists()) {
            streamOutputDirectory.mkdirs(); // 创建目录
        }

        // FFmpeg命令示例 (HLS) - 根据需要调整
        // 此命令尝试复制视频编解码器(H265)和音频编解码器。
        // 为了更广泛的兼容性，视频可能需要转码为H.264 (例如 -vcodec libx264)
        // 音频可能需要转码为AAC (例如 -acodec aac)。
        // 当前设置为转码为H.264，无音频。
        String ffmpegCommand = String.format(
            "ffmpeg -i \"%s\" " + // 输入RTSP流URL
            "-fflags flush_packets -max_delay 2 " + // FFmpeg参数，尝试减少延迟
            "-an " + // 无音频输出。如果需要音频，请移除此项并使用例如 -acodec aac 进行转码
            // "-c:v copy " + // 尝试直接复制视频流 (如果客户端支持H265 in HLS)
            "-c:v libx264 -preset ultrafast -tune zerolatency " + // 转码为H.264，使用快速预设和零延迟调整
            "-flags -global_header " + // FFmpeg参数
            "-f hls " + // 输出格式为HLS
            "-hls_time 2 " +         // HLS片段时长 (秒)
            "-hls_list_size 3 " +    // HLS播放列表最大片段数
            "-hls_wrap 3 " +         // 循环写入，旧片段会被覆盖
            "-start_number 1 " +     // HLS片段起始编号
            "-hls_segment_filename \"%s/segment%%03d.ts\" " + // HLS片段文件名模板
            "\"%s/stream.m3u8\"",    // HLS播放列表文件名
            rtspUrl,
            streamOutputDirectory.getAbsolutePath(),
            streamOutputDirectory.getAbsolutePath()
        );

        logger.info("为 streamId {} 启动 FFmpeg: {}", streamId, ffmpegCommand);

        ProcessBuilder processBuilder = new ProcessBuilder(ffmpegCommand.split(" "));
        processBuilder.redirectErrorStream(true); // 合并错误流和输出流

        try {
            Process process = processBuilder.start(); // 启动FFmpeg进程
            activeStreams.put(streamId, process); // 将活动进程存入map

            // 读取并记录FFmpeg的输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.debug("[FFmpeg {}]: {}", streamId, line); // 记录FFmpeg的每行输出
                }
            }

            int exitCode = process.waitFor(); // 等待FFmpeg进程结束
            logger.info("streamId {} 的FFmpeg进程已退出，退出码 {}", streamId, exitCode);
        } catch (IOException e) {
            logger.error("为 streamId {} 启动FFmpeg时发生IO错误: {}", streamId, e.getMessage());
            throw e; // 重新抛出异常，由Controller处理
        } catch (InterruptedException e) {
            logger.error("streamId {} 的FFmpeg进程被中断: {}", streamId, e.getMessage());
            Thread.currentThread().interrupt(); // 恢复中断状态
        } finally {
            activeStreams.remove(streamId); // 从活动流中移除此进程
            logger.info("已清理 streamId {} 的FFmpeg进程", streamId);
        }
    }

    public void stopStream(String streamId) {
        Process process = activeStreams.get(streamId);
        if (process != null && process.isAlive()) {
            logger.info("正在停止 streamId: {} 的FFmpeg进程", streamId);
            process.destroy(); // 发送 SIGTERM 信号，尝试优雅关闭
            try {
                // 等待一段时间让进程优雅关闭
                if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.warn("streamId {} 的FFmpeg进程未能优雅终止，强制停止。", streamId);
                    process.destroyForcibly(); // 发送 SIGKILL 信号，强制关闭
                }
                logger.info("streamId {} 的FFmpeg进程已停止。", streamId);
            } catch (InterruptedException e) {
                logger.error("等待FFmpeg进程 {} 停止时被中断。", streamId, e);
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
        }
        activeStreams.remove(streamId); // 从活动流中移除
    }

    public boolean isStreaming(String streamId) {
        Process process = activeStreams.get(streamId);
        return process != null && process.isAlive(); // 检查进程是否仍在运行
    }
}
