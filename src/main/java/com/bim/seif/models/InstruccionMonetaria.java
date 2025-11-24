package com.bim.seif.models;


import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "INSTRUCCION_MONETARIA")
public class InstruccionMonetaria {

    @Id
    private String folio;

    @OneToOne
    @MapsId // La clave primaria proviene de Instruccion
    @JoinColumn(name = "folio")
    private Instruccion instruccion;

    private boolean  prioritaria;
    private boolean operadaParcialmente;
    // observaciones de mesa de control cuando la instruccion esta ya finalizada
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "estatus_cve")
    private EstatusInstruccion estatus;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaClasificacion;
    private LocalDateTime fechaCancelacion;
    @PrePersist
    protected void onCreate() {
        fechaClasificacion = LocalDateTime.now();
    }


}
