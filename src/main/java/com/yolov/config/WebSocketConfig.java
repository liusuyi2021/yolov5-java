package com.yolov.config;


import com.yolov.util.websocket.handler.MessageHandler;
import com.yolov.util.websocket.interceptor.HandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private MessageHandler messageHandler;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //添加myWebSocketHandler消息处理对象以及websocket连接地址
        registry.addHandler(messageHandler, "/ws/**")
                //设置允许跨域访问
                .setAllowedOrigins("*")
                //添加拦截器可实现用户链接前进行权限校验等操作
                .addInterceptors(new HandshakeInterceptor());
    }
}