package com.bim.seif.models;

public enum Propiedad {

    fideicomiso_folio(TipoPropiedad.fideicomiso,"folio del fideicomiso")
    ,fideicomiso_alias(TipoPropiedad.fideicomiso,"nombre del fideicomiso")
    ,cuenta_numero(TipoPropiedad.cuenta,"numero de cuenta")
    ,cuenta_banco(TipoPropiedad.cuenta,"banco")
    ,cuenta_rfc(TipoPropiedad.cuenta,"rfc")
    ,cuenta_divisa(TipoPropiedad.cuenta,"divisa")
    ,divisa_tipo_cambio(TipoPropiedad.divisa,"tipo de cambio")
    ,divisa_moneda(TipoPropiedad.divisa,"moneda")
    ,divisa_llamada(TipoPropiedad.divisa,"llamada")
    ,divisa_banco(TipoPropiedad.divisa,"banco")
    ,cliente_nombre(TipoPropiedad.cliente,"nombre del cliente")
    ,cliente_email(TipoPropiedad.cliente,"email del cliente")
    ,instruccion_folio(TipoPropiedad.instruccion,"folio de la instruccion")
    ,instruccion_tipo(TipoPropiedad.instruccion,"clasificacion de la instruccion")
    ,instruccion_cliente_email(TipoPropiedad.instruccion,"correo del cliente instructor")
    ,instruccion_fecha_recepcion(TipoPropiedad.instruccion,"fecha de recepcion de la instruccion")
    ,otp(TipoPropiedad.seguridad,"One Time Password");

    private final TipoPropiedad tipoPropiedad;
    private final String descripcion;

    Propiedad(TipoPropiedad tipoPropiedad, String descripcion){
        this.tipoPropiedad = tipoPropiedad;
        this.descripcion = descripcion;
    }

    public TipoPropiedad getTipoPropiedad() {
        return tipoPropiedad;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return "{{" + name() + "}}";
    }
}
