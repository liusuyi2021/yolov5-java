package com.yolov.service;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class VideoCaptureService {

    private BlockingQueue<Mat> frameQueue = new LinkedBlockingQueue<>(100);
    static {
        // 加载opencv动态库，
        nu.pattern.OpenCV.loadLocally();
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            System.load(ClassLoader.getSystemResource("lib/opencv_videoio_ffmpeg470_64.dll").getPath());
        }
    }

    @Async("taskExecutor") // 使用配置的线程池
    public void startCapture(String videoSource) {
        VideoCapture video = new VideoCapture();
        video.open(videoSource);
        if (!video.isOpened()) {
            System.err.println("打开视频流失败");
            return;
        }

        Mat img = new Mat();
        while (video.read(img)) {
            try {
                frameQueue.put(img.clone()); // 将帧图片放入队列
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        video.release();
    }

    public BlockingQueue<Mat> getFrameQueue() {
        return frameQueue;
    }
}