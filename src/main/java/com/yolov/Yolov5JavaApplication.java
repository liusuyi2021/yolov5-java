package com.yolov;

import com.dtflys.forest.annotation.ForestClient;
import com.dtflys.forest.springboot.annotation.ForestScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ForestScan("com.yolov.forest")
public class Yolov5JavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Yolov5JavaApplication.class, args);
    }

}
