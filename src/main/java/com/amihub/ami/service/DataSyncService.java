package com.amihub.ami.service;

import com.amihub.ami.config.SyncConfig;
import com.amihub.ami.model.PriceRecord;
import com.amihub.ami.repository.PriceRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class DataSyncService {

    @Autowired private SyncConfig syncConfig;
    @Autowired private PriceRecordRepository repository;
    @Autowired private RestTemplate restTemplate;

    @Value("${api.base-url}") private String baseUrl;
    @Value("${api.key}") private String apiKey;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int LIMIT = 10; // Explicitly set to 10 as requested

    @Scheduled(cron = "0 0 9,12 * * *")
    public void scheduleDailySync() {
        log.info("Cron Job Triggered: Starting scheduled daily sync at 9AM/12PM");
        runFullDailySync();
    }
    @Async
    public void runFullDailySync() {
        long startTime = System.currentTimeMillis();
        log.info("=== SYNC START: Full Job Started ===");

        for (String state : syncConfig.getStates()) {
            syncData(state);
        }

        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        log.info("=== SYNC COMPLETE: Total time for all states: {}s ===", totalSeconds);
    }

    @Transactional
    public void syncData(String state) {
        int offset = 0;
        int totalInserted = 0, totalUpdated = 0, totalProcessed = 0;
        long stateStartTime = System.currentTimeMillis();
        boolean hasMore = true;

        log.info(">>> Syncing State: {} starts at offset 0", state);

        while (hasMore) {
            long batchStart = System.currentTimeMillis();
            String url = String.format("%s?api-key=%s&format=json&limit=%d&offset=%d&filters[state.keyword]=%s",
                    baseUrl, apiKey, LIMIT, offset, state);

            try {
                Thread.sleep(300); // Throttling for safety
                Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                List<Map<String, Object>> records = (List<Map<String, Object>>) response.get("records");

                if (records == null || records.isEmpty()) {
                    hasMore = false;
                } else {
                    for (Map<String, Object> data : records) {
                        totalProcessed++;
                        if (saveOrUpdateRecord(data)) {
                            totalInserted++;
                        } else {
                            totalUpdated++;
                        }
                    }
                    long batchTime = (System.currentTimeMillis() - batchStart);
                    log.info("State: {} | Batch at offset {} processed. Records: {}, Batch Time: {}ms | Running Totals: Processed={}, Inserted={}, Updated={}",
                            state, offset, records.size(), batchTime, totalProcessed, totalInserted, totalUpdated);

                    if (records.size() < LIMIT) {
                        hasMore = false;
                    } else {
                        offset += LIMIT;
                    }
                }
            } catch (Exception e) {
                if (e.getMessage().contains("429")) {
                    log.warn("Rate limit hit for {}. Sleeping 60s...", state);
                    try { Thread.sleep(60000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                } else {
                    log.error("Sync error for {}: {}", state, e.getMessage());
                    hasMore = false;
                }
            }
        }
        long stateSeconds = (System.currentTimeMillis() - stateStartTime) / 1000;
        log.info("<<< Finished {}: Total Processed={}, Inserted={}, Updated={} in {}s >>>",
                state, totalProcessed, totalInserted, totalUpdated, stateSeconds);
    }

    private boolean saveOrUpdateRecord(Map<String, Object> data) {
        String market = (String) data.get("market");
        String commodity = (String) data.get("commodity");
        LocalDate date = LocalDate.parse((String) data.get("arrival_date"), DATE_FORMATTER);

        Optional<PriceRecord> existingOpt = repository.findByMarketAndCommodityAndArrivalDate(market, commodity, date);
        boolean isNew = existingOpt.isEmpty();

        PriceRecord record = existingOpt.orElse(new PriceRecord());
        record.setState((String) data.get("state"));
        record.setDistrict((String) data.get("district"));
        record.setMarket(market);
        record.setCommodity(commodity);
        record.setArrivalDate(date);
        record.setMinPrice(Double.parseDouble(data.get("min_price").toString()));
        record.setMaxPrice(Double.parseDouble(data.get("max_price").toString()));
        record.setModalPrice(Double.parseDouble(data.get("modal_price").toString()));

        repository.save(record);
        return isNew;
    }
}