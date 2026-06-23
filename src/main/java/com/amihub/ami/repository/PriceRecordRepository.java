package com.amihub.ami.repository;

import com.amihub.ami.model.PriceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    // Original method for unique record lookup
    Optional<PriceRecord> findByMarketAndCommodityAndArrivalDate(String market, String commodity, LocalDate arrivalDate);

    // New method for paginated fetching (Required for Flutter bulk download)
    Page<PriceRecord> findByArrivalDate(LocalDate arrivalDate, Pageable pageable);

    // Method to leverage your idx_state_district index
    List<PriceRecord> findByStateAndDistrict(String state, String district);
}