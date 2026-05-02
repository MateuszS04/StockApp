package com.example.stockapp.controller;


import com.example.stockapp.dto.LogResponse;
import com.example.stockapp.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
public class LogController {
    private final LogService logService;

    // Stores the injected log service.
    public LogController(LogService logService){
        this.logService=logService;
    }

    // GET /log: returns the full audit log.
    @GetMapping
    public LogResponse getLog(){
        return logService.getLog();

    }
}
