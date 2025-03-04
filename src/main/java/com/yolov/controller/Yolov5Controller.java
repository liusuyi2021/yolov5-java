package com.yolov.controller;

import com.yolov.service.SimpleImageDetectionService;
import com.yolov.util.http.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(value = "yolov5")
@RestController
public class Yolov5Controller {
    @Resource
    SimpleImageDetectionService simpleImageDetectionService;

    @ApiOperation("图片识别")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam String url) {
        return AjaxResult.success(simpleImageDetectionService.run(url));
    }
}