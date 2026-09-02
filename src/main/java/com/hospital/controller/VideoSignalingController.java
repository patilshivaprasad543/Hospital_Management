package com.hospital.controller;

import com.hospital.model.VideoSignalingPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class VideoSignalingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/signal/{roomId}")
    public void handleSignal(@DestinationVariable("roomId") String roomId, @Payload VideoSignalingPayload message) {
        messagingTemplate.convertAndSend("/topic/video-room/" + roomId, message);
    }

    @MessageMapping("/chat/{roomId}")
    public void handleChat(@DestinationVariable("roomId") String roomId, @Payload VideoSignalingPayload message) {
        messagingTemplate.convertAndSend("/topic/video-chat/" + roomId, message);
        messagingTemplate.convertAndSend("/topic/video-chat/global", message);
    }
}
