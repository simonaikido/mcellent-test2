package com.bim.seif.models;
import com.google.gson.annotations.SerializedName;

public class FideicomisoConsulta {
    @SerializedName("folio")
    private String folio;

    @SerializedName("alias")
    private String alias;

    @SerializedName("region")
    private Region region;

    @SerializedName("fideicomisoContacto")
    private FideicomisoContacto fideicomisoContacto;

    @SerializedName("estado")
    private Estado estado;

    // Clases internas para los objetos anidados
    public static class Region {
        @SerializedName("cve")
        private String cve;

        @SerializedName("descripcion")
        private String descripcion;

        // Getters y Setters
        public String getCve() { return cve; }
        public void setCve(String cve) { this.cve = cve; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    }

    public static class FideicomisoContacto {
        @SerializedName("folio")
        private String folio;

        @SerializedName("nombreCliente")
        private String nombreCliente;

        @SerializedName("telefono")
        private String telefono;

        // Getters y Setters
        public String getFolio() { return folio; }
        public void setFolio(String folio) { this.folio = folio; }
        public String getNombreCliente() { return nombreCliente; }
        public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }

    public static class Estado {
        @SerializedName("cve")
        private String cve;

        @SerializedName("descripcion")
        private String descripcion;

        @SerializedName("tipo")
        private String tipo;

        // Getters y Setters
        public String getCve() { return cve; }
        public void setCve(String cve) { this.cve = cve; }
        public String getDescripcion() { return descripcion; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
    }

    // Getters y Setters para la clase principal
    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public FideicomisoContacto getFideicomisoContacto() { return fideicomisoContacto; }
    public void setFideicomisoContacto(FideicomisoContacto fideicomisoContacto) { this.fideicomisoContacto = fideicomisoContacto; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
}
