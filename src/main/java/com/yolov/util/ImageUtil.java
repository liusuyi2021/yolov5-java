package com.yolov.util;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @ClassName: ImageUtil
 * @Description:
 * @Author: liusuyi
 * @Date: 2025年02月28日10:17
 **/
public class ImageUtil {
    // 将Mat图像转换为字节数组
    public static byte[] convertMatToBytes(Mat img) {
        MatOfByte matOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, matOfByte); // 可以根据需求选择不同的格式：.jpg, .png
        return matOfByte.toArray(); // 转换为字节数组
    }

    public static Mat readImageFromURL(String imageUrl) throws IOException {
        // 通过 URL 下载图像数据
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.connect();

        // 获取输入流
        InputStream inputStream = connection.getInputStream();

        // 将输入流数据读取到字节数组
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        // 关闭流
        inputStream.close();

        // 使用字节数据创建 Mat 对象
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        MatOfByte matOfByte = new MatOfByte(byteArray);

        // 使用 imdecode 来读取图像
        Mat img = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);

        return img;
    }
    // 将Mat转换为BufferedImage
    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int type = BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage image = new BufferedImage(width, height, type);
        mat.get(0, 0, ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData());
        return image;
    }
    // 将BufferedImage转换为Mat
    public static Mat bufferedImageToMat(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int type = CvType.CV_8UC3; // 3通道图像
        Mat mat = new Mat(height, width, type);
        byte[] data = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    public static float[] whc2cwh(float[] src) {
        float[] chw = new float[src.length];
        int j = 0;
        for (int ch = 0; ch < 3; ++ch) {
            for (int i = ch; i < src.length; i += 3) {
                chw[j] = src[i];
                j++;
            }
        }
        return chw;
    }
}
