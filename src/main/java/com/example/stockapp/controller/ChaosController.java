package com.example.stockapp.controller;


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

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void chaos(){
        log.warn("Chaos endpoint invoked - this instance will stop in 100ms");

        Thread killer = new Thread(()-> {
            try{
                Thread.sleep(100);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(1);
        },
                "chaos-killer");
        killer.setDaemon(false);
        killer.start();
    }
}
