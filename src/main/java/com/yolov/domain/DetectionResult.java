package com.yolov.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @ClassName: DetectionResult
 * @Description:
 * @Author: liusuyi
 * @Date: 2025年02月28日11:11
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DetectionResult {
    private List<Detection> detections;
    private String resultImage;
}
