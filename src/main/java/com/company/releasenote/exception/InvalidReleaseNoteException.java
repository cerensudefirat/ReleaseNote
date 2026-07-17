package com.company.releasenote.exception;

public class InvalidReleaseNoteException extends RuntimeException{
    public InvalidReleaseNoteException(String message){
        super(message);
    }
}
