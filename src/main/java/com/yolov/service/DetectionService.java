package com.yolov.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.yolov.config.ODConfig;
import com.yolov.domain.Letterbox;
import com.yolov.domain.ODResult;
import com.yolov.util.DetectionUtil;
import com.yolov.util.ImageUtil;
import com.yolov.util.websocket.utils.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DetectionService {
    static Map<String, Integer> last = new ConcurrentHashMap<>();
    static Map<String, Integer> current = new ConcurrentHashMap<>();

    static Map<String, Integer> count = new ConcurrentHashMap<>();

    private final OrtEnvironment environment = OrtEnvironment.getEnvironment();
    private OrtSession session;
    private final String[] labels = {
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train",
            "truck", "boat", "traffic light", "fire hydrant", "stop sign", "parking meter",
            "bench", "bird", "cat", "dog", "horse", "sheep", "cow", "elephant", "bear",
            "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase",
            "frisbee", "skis", "snowboard", "sports ball", "kite", "baseball bat",
            "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
            "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut",
            "cake", "chair", "couch", "potted plant", "bed", "dining table", "toilet",
            "tv", "laptop", "mouse", "remote", "keyboard", "cell phone", "microwave",
            "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors",
            "teddy bear", "hair drier", "toothbrush"};

    public DetectionService() throws OrtException {
        String model_path = "src/main/resources/model/yolov7-tiny.onnx";
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
        sessionOptions.addCUDA(0); // 使用GPU
        session = environment.createSession(model_path, sessionOptions);
    }

    @Async("taskExecutor") // 使用配置的线程池
    public void startDetection(BlockingQueue<Mat> frameQueue) {
        Letterbox letterbox = new Letterbox();
        ODConfig odConfig = new ODConfig();
        int detect_skip = 3;
        int detect_skip_index = 1;
        float[][] outputData = null;

        while (true) {
            try {
                Mat image = frameQueue.take(); // 从队列中取出帧图片
                if ((detect_skip_index % detect_skip == 0) || outputData == null) {
                    image = letterbox.letterbox(image);
                    Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);
                    image.convertTo(image, CvType.CV_32FC1, 1. / 255);
                    float[] whc = new float[3 * 640 * 640];
                    image.get(0, 0, whc);
                    float[] chw = ImageUtil.whc2cwh(whc);

                    detect_skip_index = 1;
                    FloatBuffer inputBuffer = FloatBuffer.wrap(chw);
                    OnnxTensor tensor = OnnxTensor.createTensor(environment, inputBuffer, new long[]{1, 3, 640, 640});

                    HashMap<String, OnnxTensor> inputMap = new HashMap<>();
                    inputMap.put(session.getInputInfo().keySet().iterator().next(), tensor);

                    OrtSession.Result output = session.run(inputMap);
                    outputData = (float[][]) output.get(0).getValue();
                } else {
                    detect_skip_index++;
                }

                // 处理检测结果
                processDetectionResult(image, outputData, letterbox, odConfig);

            } catch (InterruptedException | OrtException e) {
                e.printStackTrace();
            }
        }
    }
    @Scheduled()
    private void heart()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type","boxName");
        map.put("coordinates","[0,0,0,0]");
        WebSocketUtil.sendMessageAll(map);
    }

    private void processDetectionResult(Mat img, float[][] outputData, Letterbox letterbox, ODConfig odConfig) {
        // 处理检测结果的逻辑 例如：画框、告警判断等
        int fontFace = Imgproc.FONT_HERSHEY_SIMPLEX;
        int thickness = 3;
        current.clear();
        for (float[] x : outputData) {

            ODResult odResult = new ODResult(x);
            // 业务逻辑写在这里，注释下面代码，增加自己的代码，根据返回识别到的目标类型，编写告警逻辑。等等
            String boxName = labels[odResult.getClsId()];
            if (!boxName.equals("person") && !boxName.equals("cell phone")) {
                continue;
            }
            if (current.containsKey(boxName)) {
                current.put(boxName, current.get(boxName) + 1);
            } else {
                current.put(boxName, 1);
            }
            // 实际项目中建议不要在视频画面上画框和文字，只告警，或者在告警图片上画框。画框和文字对视频帧率影响非常大
            // 画框
            Point topLeft = new Point((odResult.getX0() - letterbox.getDw()) / letterbox.getRatio(),
                    (odResult.getY0() - letterbox.getDh()) / letterbox.getRatio());
            Point bottomRight = new Point((odResult.getX1() - letterbox.getDw()) / letterbox.getRatio(),
                    (odResult.getY1() - letterbox.getDh()) / letterbox.getRatio());
            Scalar color = new Scalar(odConfig.getOtherColor(odResult.getClsId()));

            Imgproc.rectangle(img, topLeft, bottomRight, color, thickness);

            // 框上写文字
            Point boxNameLoc = new Point((odResult.getX0() - letterbox.getDw()) / letterbox.getRatio(),
                    (odResult.getY0() - letterbox.getDh()) / letterbox.getRatio() - 3);

            // 也可以二次往视频画面上叠加其他文字或者数据，比如物联网设备数据等等
            Imgproc.putText(img, boxName, boxNameLoc, fontFace, 0.7, color, thickness);

            // 打印目标的坐标
            float[] xy = DetectionUtil.convertBboxToOriginalCoordinates(Arrays.copyOfRange(x, 1, 5),
                    letterbox.getRatio(), letterbox.getDw(),
                    letterbox.getDh());
            log.warn("{}--- coordinates: {}", boxName, xy);
            Map<String, Object> map = new HashMap<>();
            map.put("type",boxName);
            map.put("coordinates",xy);
            WebSocketUtil.sendMessageAll(map);
//            // 保存带目标框的图像到本地
//            String outputFilePath = "output_image.jpg";  // 你可以自定义路径和文件名
//            boolean isSaved = Imgcodecs.imwrite(outputFilePath, img);  // 保存图像
//            if (isSaved) {
//                log.info("Image with detection boxes saved to: {}", outputFilePath);
//            } else {
//                log.error("Failed to save image to: {}", outputFilePath);
//            }
        }

        for (Map.Entry<String, Integer> entry : last.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                log.warn("{}个 {} 离开了", entry.getValue(), entry.getKey());
                count.remove(entry.getKey());
            }
        }

        for (Map.Entry<String, Integer> entry : current.entrySet()) {
            int lastCount = last.get(entry.getKey()) == null ? 0 : entry.getValue();
            int currentCount = entry.getValue();
            if ((lastCount < currentCount)) {
                log.warn("{}个 {} 出现了", +(currentCount - lastCount), entry.getKey());
            }
        }
        last.clear();
        last.putAll(current);
    }
}