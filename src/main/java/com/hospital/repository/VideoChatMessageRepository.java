package com.hospital.repository;

import com.hospital.model.VideoChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoChatMessageRepository extends JpaRepository<VideoChatMessage, Long> {
    List<VideoChatMessage> findByRoomIdOrderBySentAtAsc(String roomId);
}
