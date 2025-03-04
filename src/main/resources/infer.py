import onnxruntime as ort
import cv2
import sys
import numpy as np

def preprocess(image):
    image = cv2.resize(image, (640, 640))
    image = image / 255.0
    image = np.transpose(image, (2, 0, 1))
    image = np.expand_dims(image, axis=0)
    return image.astype(np.float32)

def infer(image_path, model_path):
    session = ort.InferenceSession(model_path)  # 使用传入的模型路径
    image = cv2.imread(image_path)
    input_data = preprocess(image)
    input_name = session.get_inputs()[0].name
    outputs = session.run(None, {input_name: input_data})
    return outputs

if __name__ == "__main__":
    image_path = sys.argv[1]  # 从命令行参数获取图片路径
    model_path = sys.argv[2]  # 从命令行参数获取模型路径
    result = infer(image_path, model_path)
    print(result)  # 打印推理结果
