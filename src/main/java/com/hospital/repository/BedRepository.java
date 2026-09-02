package com.hospital.repository;

import com.hospital.model.Bed;
import com.hospital.model.BedStatus;
import com.hospital.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {
    List<Bed> findByRoom(Room room);
    List<Bed> findByStatus(BedStatus status);
    List<Bed> findByStatusOrderByBedNumberAsc(BedStatus status);
    List<Bed> findByRoomWardId(Long wardId);
    List<Bed> findAllByOrderByRoomWardNameAscRoomRoomNumberAscBedNumberAsc();
    long countByStatus(BedStatus status);
}
