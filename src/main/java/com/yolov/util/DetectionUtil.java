package com.yolov.util;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: DetectionUtil
 * @Description:
 * @Author: liusuyi
 * @Date: 2025年02月28日10:20
 **/
public class DetectionUtil {
    //返回最大值的索引
    public static int argmax(float[] a) {
        float re = -Float.MAX_VALUE;
        int arg = -1;
        for (int i = 0; i < a.length; i++) {
            if (a[i] >= re) {
                re = a[i];
                arg = i;
            }
        }
        return arg;
    }

    public static void xywh2xyxy(float[] bbox) {
        float x = bbox[0];
        float y = bbox[1];
        float w = bbox[2];
        float h = bbox[3];

        bbox[0] = x - w * 0.5f;
        bbox[1] = y - h * 0.5f;
        bbox[2] = x + w * 0.5f;
        bbox[3] = y + h * 0.5f;
    }

    public static List<float[]> nonMaxSuppression(List<float[]> bboxes, float iouThreshold) {

        List<float[]> bestBboxes = new ArrayList<>();

        bboxes.sort(Comparator.comparing(a -> a[4]));

        while (!bboxes.isEmpty()) {
            float[] bestBbox = bboxes.remove(bboxes.size() - 1);
            bestBboxes.add(bestBbox);
            bboxes = bboxes.stream().filter(a -> computeIOU(a, bestBbox) < iouThreshold).collect(Collectors.toList());
        }

        return bestBboxes;
    }

    public static float computeIOU(float[] box1, float[] box2) {

        float area1 = (box1[2] - box1[0]) * (box1[3] - box1[1]);
        float area2 = (box2[2] - box2[0]) * (box2[3] - box2[1]);

        float left = Math.max(box1[0], box2[0]);
        float top = Math.max(box1[1], box2[1]);
        float right = Math.min(box1[2], box2[2]);
        float bottom = Math.min(box1[3], box2[3]);

        float interArea = Math.max(right - left, 0) * Math.max(bottom - top, 0);
        float unionArea = area1 + area2 - interArea;
        return Math.max(interArea / unionArea, 1e-8f);

    }

    public static Point2D convertToOriginalCoordinates(double x, double y, double ratio, double dw, double dh) {
        double originalX = (x - dw) / ratio;
        double originalY = (y - dh) / ratio;
        return new Point2D.Double(originalX, originalY);
    }

    public static float[] convertBboxToOriginalCoordinates(float[] bbox, double ratio, double dw, double dh) {

        // 计算原始坐标
        double x1 = (bbox[0] - dw) / ratio; // 左上角 x
        double y1 = (bbox[1] - dh) / ratio; // 左上角 y
        double x2 = (bbox[2] - dw) / ratio; // 右下角 x
        double y2 = (bbox[3] - dh) / ratio; // 右下角 y

        // 将 double 转换为 float 并返回新的边界框坐标
        return new float[]{(float) x1, (float) y1, (float) x2, (float) y2};
    }
}
