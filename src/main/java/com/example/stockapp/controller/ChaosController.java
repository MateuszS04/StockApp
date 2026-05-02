package com.example.stockapp.controller;


import com.example.stockapp.service.JvmHalter;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/chaos")
public class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);
    private final JvmHalter jvmHalter;

    // Stores the injected JVM halter.
    public ChaosController(JvmHalter jvmHalter) {
        this.jvmHalter = jvmHalter;
    }

    // POST /chaos: schedules this instance to die.
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void chaos() {
        log.warn("Chaos endpoint invoked - this instance will stop in 100ms");
        jvmHalter.haltAfterDelay();
    }
}
