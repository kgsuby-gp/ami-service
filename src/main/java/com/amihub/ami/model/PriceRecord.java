package com.amihub.ami.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "price_records", indexes = {
        @Index(name = "idx_market_commodity_date", columnList = "market, commodity, arrival_date", unique = true),
        @Index(name = "idx_state_district", columnList = "state, district")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String market;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private LocalDate arrivalDate;

    private Double minPrice;
    private Double maxPrice;
    private Double modalPrice;
}