package com.final_year.v2.controller;

import com.final_year.v2.dto.HistoryResponse;
import com.final_year.v2.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ResponseEntity<Page<HistoryResponse>> getUserHistory(Pageable pageable) {
        return ResponseEntity.ok(historyService.getUserHistory(pageable));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearHistory() {
        historyService.clearHistory();
        return ResponseEntity.noContent().build();
    }
}