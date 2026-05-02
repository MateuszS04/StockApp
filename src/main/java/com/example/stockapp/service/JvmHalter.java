package com.example.stockapp.service;


import org.springframework.stereotype.Component;

@Component
public class JvmHalter {

    // Hard-kills the JVM after a small delay.
    public void haltAfterDelay(){
        Thread killer= new Thread(()->{
            try{
                Thread.sleep(100);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
            Runtime.getRuntime().halt(1);
        },
                "Chaos-killer");
        killer.setDaemon(false);
        killer.start();
    }
}
