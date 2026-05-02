package com.example.stockapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.LinkedHashMap;
import java.time.Instant;




@RestControllerAdvice
public class GlobalExceptionHandler  {
   // Maps unknown-stock errors to HTTP 404.
   @ExceptionHandler(StockNotKnownException.class)
    public ResponseEntity<Map<String,Object>>handleStockNotKnown(StockNotKnownException ex){
       return error(HttpStatus.NOT_FOUND, ex.getMessage());
       //Stock name not in bank:known_stocks, response 404 + JSON body
   }

    // Maps unknown-wallet errors to HTTP 404.
    @ExceptionHandler(WalletNotKnownException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,String> handleWalletNotKnown(WalletNotKnownException ex){
        return Map.of("error", ex.getMessage());
    }

   // Maps insufficient-stock errors to HTTP 400.
   @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String,Object>>handleInsufficient(InsufficientStockException ex){
       return error(HttpStatus.BAD_REQUEST, ex.getMessage());
       // Lua returned 0 + 400 + JSON body
   }

    // Maps bean-validation failures to HTTP 400.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex){
       Map<String, String> fields = new LinkedHashMap<>();

       for(FieldError fe : ex.getBindingResult().getFieldErrors()){
           fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
       }

       Map<String, Object> body= baseBody(HttpStatus.BAD_REQUEST, "Validation failed");
       body.put("fields", fields);
       return ResponseEntity.badRequest().body(body);
       //@Valdi failed 400+ per-field errors

    }

    // Maps malformed JSON / bad enum to HTTP 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex){
       return error(HttpStatus.BAD_REQUEST, "Malformed JSON or invalid value");
       // JSON malformed or type value not in enum, response 400 + generic message
    }

    // Wraps a status and message in an error ResponseEntity.
    private ResponseEntity<Map<String,Object>> error(HttpStatus status, String message){
       return ResponseEntity.status(status).body(baseBody(status, message));
    }

    // Builds the standard error body skeleton.
    private Map<String,Object> baseBody(HttpStatus status, String message){
       Map<String,Object> body = new LinkedHashMap<>();
       body.put("timestamp", Instant.now().toString());
       body.put("status",status.value());
       body.put("error", status.getReasonPhrase());
       body.put("message", message);
       return body;
    }
}
