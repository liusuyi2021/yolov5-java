package com.yolov.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import cn.hutool.core.date.StopWatch;
import com.yolov.config.ODConfig;
import com.yolov.domain.Detection;
import com.yolov.domain.DetectionResult;
import com.yolov.domain.Letterbox;
import com.yolov.minio.MinioUtil;
import com.yolov.util.DetectionUtil;
import com.yolov.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

import static com.yolov.util.ImageUtil.bufferedImageToMat;

/**
 * ClassName: SimpleImageDetectionService
 * Description: 单张图片检测服务
 * Author: liusuyi
 * Date: 2025年02月28日10:13
 **/
@Service
@Slf4j
public class SimpleImageDetectionService {


    private OrtEnvironment environment;
    private OrtSession session;
    String DefaultImagePath = "src\\main\\resources\\default_image.jpg";
    String model_path = "src\\main\\resources\\model\\best.onnx";
    String[] labels = {"桶", "电盒", "开关", "锁"};

    float confThreshold = 0.35F;
    float nmsThreshold = 0.45F;
    ODConfig odConfig = new ODConfig();

    static {
        // 加载opencv动态库，
        nu.pattern.OpenCV.loadLocally();
    }

    @PostConstruct
    public void init() throws OrtException {
        environment = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        sessionOptions.addCUDA(0);
        session = environment.createSession(model_path, sessionOptions);
        initRun();
    }

    public void initRun() {
        try {
            Mat img = Imgcodecs.imread(DefaultImagePath);
            DetectionResult result = processImage(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DetectionResult run(String imageUrl) {
        try {
            Mat img = ImageUtil.readImageFromURL(imageUrl);
            return processImage(img);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private DetectionResult processImage(Mat img) throws OrtException {

        Mat image = img.clone();
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);
        int minDwDh = Math.min(img.width(), img.height());
        int thickness = minDwDh / 400;

        Letterbox letterbox = new Letterbox();
        image = letterbox.letterbox(image);

        double ratio = letterbox.getRatio();
        double dw = letterbox.getDw();
        double dh = letterbox.getDh();
        int rows = letterbox.getHeight();
        int cols = letterbox.getWidth();
        int channels = image.channels();

        float[] pixels = new float[channels * rows * cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] pixel = image.get(j, i);
                for (int k = 0; k < channels; k++) {
                    pixels[rows * cols * k + j * cols + i] = (float) pixel[k] / 255.0f;
                }
            }
        }

        long[] shape = {1L, (long) channels, (long) rows, (long) cols};
        OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(pixels), shape);
        HashMap<String, OnnxTensor> stringOnnxTensorHashMap = new HashMap<>();
        stringOnnxTensorHashMap.put(session.getInputInfo().keySet().iterator().next(), tensor);

        OrtSession.Result output = session.run(stringOnnxTensorHashMap);

        float[][] outputData = ((float[][][]) output.get(0).getValue())[0];
        Map<Integer, List<float[]>> class2Bbox = new HashMap<>();
        for (float[] bbox : outputData) {
            float score = bbox[4];
            if (score < confThreshold) continue;

            float[] conditionalProbabilities = Arrays.copyOfRange(bbox, 5, bbox.length);
            int label = DetectionUtil.argmax(conditionalProbabilities);

            DetectionUtil.xywh2xyxy(bbox);

            if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3]) continue;

            class2Bbox.putIfAbsent(label, new ArrayList<>());
            class2Bbox.get(label).add(bbox);
        }

        List<Detection> detections = new ArrayList<>();
        for (Map.Entry<Integer, List<float[]>> entry : class2Bbox.entrySet()) {
            List<float[]> bboxes = entry.getValue();
            bboxes = DetectionUtil.nonMaxSuppression(bboxes, nmsThreshold);
            for (float[] bbox : bboxes) {
                String labelString = labels[entry.getKey()];
                detections.add(new Detection(labelString, entry.getKey(), Arrays.copyOfRange(bbox, 0, 4), bbox[4]));
            }
        }
        for (Detection detection : detections) {
            float[] bbox = detection.getBbox();
            String boxName = labels[detection.getClsId()];
            Point topLeft = new Point((bbox[0] - dw) / ratio, (bbox[1] - dh) / ratio);
            Point bottomRight = new Point((bbox[2] - dw) / ratio, (bbox[3] - dh) / ratio);
            Scalar color = new Scalar(odConfig.getOtherColor(detection.getClsId()));
            Imgproc.rectangle(img, topLeft, bottomRight, color, thickness);

            BufferedImage bufferedImage = ImageUtil.matToBufferedImage(img);
            Graphics2D g2d = bufferedImage.createGraphics();
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 20));
            g2d.setColor(Color.RED);

            int textX = (int) topLeft.x;
            int textY = (int) topLeft.y - 3;
            if (textY < 0) textY = (int) topLeft.y + 20;

            g2d.drawString(boxName, textX, textY);
            g2d.dispose();

            img = ImageUtil.bufferedImageToMat(bufferedImage);
            //转换成实际坐标
            float[] xy = DetectionUtil.convertBboxToOriginalCoordinates(detection.getBbox(),ratio, dw, dh);
            detection.setBbox(xy);
            log.warn(Arrays.toString(xy));

        }

//        byte[] imgBytes = ImageUtil.convertMatToBytes(img);
//        InputStream is = new ByteArrayInputStream(imgBytes);
//        String resultUrl = "";
//        String bucket = "pic";
//        String object = UUID.randomUUID() + ".jpg";
//        boolean pic = MinioUtil.uploadObject("pic", object, is, imgBytes.length, "image/jpeg");
//        if (pic) {
//            resultUrl = MinioUtil.getBucketObjectUrl(bucket, object);
//        }
//        return DetectionResult.builder().detections(detections).resultImage(resultUrl).build();
        return DetectionResult.builder().detections(detections).build();
    }
}