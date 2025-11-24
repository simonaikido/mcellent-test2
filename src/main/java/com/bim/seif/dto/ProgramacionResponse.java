package com.bim.seif.dto;

import lombok.Data;



@Data
public class ProgramacionResponse {
    private String cuentaCargo;
    private String cuentaAbono;
    private Integer monto; // Ajusta según lo que sean 454156
    private String fechaProgramada;

    // Constructor
    public ProgramacionResponse(String cuentaCargo, String cuentaAbono, Integer monto, String fechaProgramada) {
        this.cuentaCargo = cuentaCargo;
        this.cuentaAbono = cuentaAbono;
        this.monto = monto;
        this.fechaProgramada = fechaProgramada;
    }

    public String getCuentaCargo() {
        return cuentaCargo;
    }

    public void setCuentaCargo(String cuentaCargo) {
        this.cuentaCargo = cuentaCargo;
    }

    public String getCuentaAbono() {
        return cuentaAbono;
    }

    public void setCuentaAbono(String cuentaAbono) {
        this.cuentaAbono = cuentaAbono;
    }

    public Integer getMonto() {
        return monto;
    }

    public void setMonto(Integer monto) {
        this.monto = monto;
    }

    public String getFechaProgramada() {
        return fechaProgramada;
    }

    public void setFechaProgramada(String fechaProgramada) {
        this.fechaProgramada = fechaProgramada;
    }
}