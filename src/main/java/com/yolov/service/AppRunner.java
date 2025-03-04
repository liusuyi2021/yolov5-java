package com.yolov.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppRunner implements CommandLineRunner {

    @Autowired
    private VideoCaptureService videoCaptureService;

    @Autowired
    private DetectionService detectionService;

    @Override
    public void run(String... args) throws Exception {
        String videoSource = "rtsp://ceshi:zdkj.2022@192.168.1.237:554/h264/ch1/main/av_stream";
        videoCaptureService.startCapture(videoSource);
        detectionService.startDetection(videoCaptureService.getFrameQueue());
    }
}