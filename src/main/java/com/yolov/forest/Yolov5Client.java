package com.yolov.forest;

import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.Body;
import com.dtflys.forest.annotation.Post;

/**
 * @ClassName:Yolov5Client
 * @Description:
 * @Author:ard
 * @Date:2024年05月23日15:54
 * @Version:1.0
 **/
@BaseRequest(baseURL = "http://127.0.0.1:5000")
public interface Yolov5Client {
    @Post(value = "/detect", contentType = "text/plain")
    byte[] detect(@Body String url);
}
