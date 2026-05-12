package com.victor.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MapPointDTO {
    private double lat;
    private double lng;
    private String destination;
    private long   count;                  // total orders to this destination
    private Map<String, Long> statuses;    // count per status (DELIVERED, SHIPPED, etc.)
}
