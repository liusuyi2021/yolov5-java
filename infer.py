import onnxruntime as ort
import cv2
import sys
import numpy as np

def preprocess(image):
    # 调整图像大小为 YOLO 模型的输入尺寸
    image = cv2.resize(image, (640, 640))
    image = image / 255.0  # 归一化
    image = np.transpose(image, (2, 0, 1))  # 转换为 (C, H, W)
    image = np.expand_dims(image, axis=0)  # 添加批次维度
    return image.astype(np.float32)

def draw_boxes(image, boxes, confidences, class_ids, class_names, confidence_threshold=0.5):
    # 根据检测框绘制框和标签
    for i in range(len(boxes)):
        if confidences[i] > confidence_threshold:
            x1, y1, x2, y2 = boxes[i]
            label = f"{class_names[class_ids[i]]} ({confidences[i]:.2f})"

            # 绘制框
            cv2.rectangle(image, (x1, y1), (x2, y2), (0, 255, 0), 2)

            # 添加标签
            cv2.putText(image, label, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
    return image

def infer(image_path, model_path, output_path):
    # 加载 YOLO 模型
    session = ort.InferenceSession(model_path)  # 使用传递的模型路径
    image = cv2.imread(image_path)

    # 预处理图像
    input_data = preprocess(image)
    input_name = session.get_inputs()[0].name
    outputs = session.run(None, {input_name: input_data})

    # 处理模型输出
    boxes, confidences, class_ids = postprocess(outputs, image.shape[1], image.shape[0])

    # 绘制检测框
    class_names = ['plate']  # 你需要根据你的模型修改类别名称
    image_with_boxes = draw_boxes(image, boxes, confidences, class_ids, class_names)

    # 保存带框的图像
    cv2.imwrite(output_path, image_with_boxes)
    return output_path

def postprocess(outputs, image_width, image_height, confidence_threshold=0.5):
    boxes = []
    confidences = []
    class_ids = []

    # outputs的形状为 (1, N, 6)，每行是 [x1, y1, x2, y2, confidence, class_id]
    for output in outputs[0]:
        if output[4] > confidence_threshold:  # 如果置信度大于阈值
            # 获取检测框坐标，归一化坐标，转化为原图像的坐标
            x1 = int(output[0] * image_width)
            y1 = int(output[1] * image_height)
            x2 = int(output[2] * image_width)
            y2 = int(output[3] * image_height)
            boxes.append([x1, y1, x2, y2])
            confidences.append(output[4])
            class_ids.append(int(output[5]))  # 类别ID

    return boxes, confidences, class_ids

if __name__ == "__main__":
    image_path = sys.argv[1]  # 从命令行参数获取图片路径
    model_path = sys.argv[2]  # 从命令行参数获取模型路径
    output_path = sys.argv[3]  # 从命令行参数获取输出路径
    infer(image_path, model_path, output_path)
