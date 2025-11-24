package com.bim.seif.exceptions;

public class InactiveUserException extends Exception {
    public InactiveUserException(String mensaje){
        super(mensaje);
    }
}
