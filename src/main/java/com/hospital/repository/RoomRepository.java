package com.hospital.repository;

import com.hospital.model.Room;
import com.hospital.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByWard(Ward ward);
}
