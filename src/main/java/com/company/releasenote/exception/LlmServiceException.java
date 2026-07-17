package com.company.releasenote.exception;

public class LlmServiceException extends RuntimeException{
    public LlmServiceException(String message,Throwable cause){
        super(message, cause);
    }
}
