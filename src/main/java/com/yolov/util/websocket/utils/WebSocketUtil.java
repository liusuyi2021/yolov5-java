package com.yolov.util.websocket.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @ClassName WebSocketUtils
 * @Description: websocket工具类
 * @Author 刘苏义
 * @Date 2023/1/27 7:46
 * @Version 1.0
 */
@Slf4j
public final class WebSocketUtil {

    // 存储 websocket session
    public static final ConcurrentMap<Long, WebSocketSession> ONLINE_USER_SESSIONS = new ConcurrentHashMap<>();
    /**
     * @param session 用户 session
     * @param message 发送内容
     */
    public static void sendMessage(WebSocketSession session, String message) {
        if (session == null) {
            return;
        }
        final InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress == null) {
            return;
        }
        synchronized (session) {
            try {
                log.debug("发送消息：" + message);
                session.sendMessage(new TextMessage(String.join(", ", message)));
            } catch (IOException e) {
                log.error("sendMessage IOException ", e);
            }
        }
    }

    public static void sendMessage(Long userId, String message) {
        WebSocketSession session = ONLINE_USER_SESSIONS.get(userId);
        sendMessage(session, message);
    }

    /**
     * @param session 用户 session
     * @param message 发送内容
     */
    public static void sendMessage(WebSocketSession session, Map message) {
        if (session == null) {
            return;
        }
        final InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress == null) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSONObject.toJSONString(message)));
            } catch (IOException e) {
                log.error("sendMessage IOException ", e);
            }
        }
    }

    public static void sendMessage(WebSocketSession session, List message) {
        if (session == null) {
            return;
        }
        final InetSocketAddress remoteAddress = session.getRemoteAddress();
        if (remoteAddress == null) {
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSONObject.toJSONString(message)));
            } catch (IOException e) {
                log.error("sendMessage IOException ", e);
            }
        }
    }

    /**
     * 推送消息到其他客户端
     *
     * @param message
     */
    public static void sendMessageAll(String message) {
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, message));
    }

    /**
     * 推送消息到其他客户端
     *
     * @param message
     */
    public static void sendMessageAll(Map message) {
        JSONObject jsonObject = new JSONObject(message);
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, jsonObject.toString()));
    }

    /**
     * 推送消息到其他客户端
     *
     * @param message
     */
    public static void sendMessageAll(Object message) {
        String jsonMessage = JSONObject.toJSONString(message);  // 确保对象转换为 JSON 字符串
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, jsonMessage));
    }

    /**
     * 推送消息到其他客户端
     *
     * @param message
     */
    public static void sendMessageAll(List message) {
        ONLINE_USER_SESSIONS.forEach((sessionId, session) -> sendMessage(session, JSON.toJSONString(message)));
    }

}