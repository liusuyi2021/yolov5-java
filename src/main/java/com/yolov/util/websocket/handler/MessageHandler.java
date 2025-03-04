package com.yolov.util.websocket.handler;

import com.yolov.util.websocket.utils.WebSocketUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;


@Component
@Slf4j
public class MessageHandler extends TextWebSocketHandler {



    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 处理接收到的消息
        String userIdStr  = (String) session.getAttributes().get("userId");
        Long userId = Long.parseLong(userIdStr);
        String payload = message.getPayload();
        log.info("【cmd】Received message: {}", payload);
        log.info("【cmd】size: {}", WebSocketUtil.ONLINE_USER_SESSIONS.size());

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("【cmd】Connected: {}", session.getId());
        String userIdStr  = (String) session.getAttributes().get("userId");
        Long userId = Long.parseLong(userIdStr);
        WebSocketUtil.ONLINE_USER_SESSIONS.put(userId, session);
        log.info("【cmd】size: {}", WebSocketUtil.ONLINE_USER_SESSIONS.size());

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("【cmd】Disconnected: {}", session.getId());
        String userIdStr  = (String) session.getAttributes().get("userId");
        Long userId = Long.parseLong(userIdStr);
        WebSocketUtil.ONLINE_USER_SESSIONS.remove(userId);
        log.info("【cmd】size: {}", WebSocketUtil.ONLINE_USER_SESSIONS.size());

    }
}