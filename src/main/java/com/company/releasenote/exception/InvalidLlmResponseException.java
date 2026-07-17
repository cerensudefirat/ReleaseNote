package com.company.releasenote.exception;

public class InvalidLlmResponseException extends RuntimeException{
    public InvalidLlmResponseException(String message){
        super(message);
    }
    public InvalidLlmResponseException(String message,Throwable cause){
        super(message, cause);
    }
}
