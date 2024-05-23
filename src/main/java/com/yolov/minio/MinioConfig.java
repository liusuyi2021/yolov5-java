package com.yolov.minio;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description:
 * @ClassName: MinioInit
 * @Author: 刘苏义
 * @Date: 2023年08月25日14:03:45
 **/

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    MinioUtil getMinioUtil()
    {
        log.info("初始化minio配置"+"【"+endpoint+"("+accessKey+"/"+secretKey+")】");
        return new MinioUtil(endpoint,accessKey,secretKey);
    }
}
