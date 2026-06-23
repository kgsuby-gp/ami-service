package com.amihub.ami.controller;

import com.amihub.ami.model.PriceRecord;
import com.amihub.ami.repository.PriceRecordRepository;
import com.amihub.ami.service.DataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private PriceRecordRepository repository;

    @Autowired
    private DataSyncService dataSyncService;

    // Flutter calls this to get today's records in chunks of 1000
    // Usage: /api/sync/all-daily-records?page=0&size=1000
    @GetMapping("/all-daily-records")
    public List<PriceRecord> getAllDailyRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size) {

        return repository.findByArrivalDate(
                LocalDate.now(),
                PageRequest.of(page, size)
        ).getContent();
    }

    // Standard POST triggers
    @PostMapping("/trigger")
    public String triggerSync() {
        dataSyncService.runFullDailySync();
        return "Sync initiated successfully for all configured states.";
    }

    @PostMapping("/trigger-manual")
    public String triggerManualSync() {
        dataSyncService.runFullDailySync();
        return "Sync started in a managed background thread.";
    }
}