package com.hospital.repository;

import com.hospital.model.OtpCode;
import com.hospital.model.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findByLookupKeyAndPurpose(String lookupKey, OtpPurpose purpose);

    void deleteByLookupKeyAndPurpose(String lookupKey, OtpPurpose purpose);
}
