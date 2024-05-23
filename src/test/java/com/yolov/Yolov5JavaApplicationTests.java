package com.yolov;

import com.yolov.forest.Yolov5Client;
import com.yolov.minio.MinioUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

@SpringBootTest
class Yolov5JavaApplicationTests {

    @Resource
    Yolov5Client yolov5Client;

    @Test
    void contextLoads() {
        byte[] imageBytes = yolov5Client.detect("http://192.168.2.15:9001/liusuyi/5.jpg");
        InputStream inputStream = new ByteArrayInputStream(imageBytes);
        // 将图片上传至minio
        String bucket = "liusuyi";
        String object = UUID.randomUUID() + ".jpg";
        boolean b = MinioUtil.uploadObject(bucket, object, inputStream, imageBytes.length, "image/jpeg");
        String bucketObjectUrl = MinioUtil.getBucketObjectUrl(bucket, object);
        System.out.println(bucketObjectUrl);
    }

}
