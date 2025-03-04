package com.yolov.util.websocket.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * websocket基础消息
 *
 * @author liusuyi
 * @date 2024年11月04日14:40
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BaseMessage {
    private String type;    // 消息类型
    private String payload;  // 消息内容
}
