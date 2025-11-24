package com.bim.seif.exceptions;

public class BadAuthException extends Exception{
    public BadAuthException(String mensaje){
        super(mensaje);
    }
}
