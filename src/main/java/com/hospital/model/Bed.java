package com.hospital.model;

import jakarta.persistence.*;

@Entity
@Table(name = "beds")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String bedNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedStatus status;

    public Bed() {
        this.status = BedStatus.AVAILABLE;
    }

    public Bed(String bedNumber, Room room, BedStatus status) {
        this.bedNumber = bedNumber;
        this.room = room;
        this.status = status != null ? status : BedStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public BedStatus getStatus() { return status; }
    public void setStatus(BedStatus status) { this.status = status; }
}
