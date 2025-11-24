package com.bim.seif.models;


import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "solicitud_cuenta")
public class SolicitudCuenta {

    @Id
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instruccion_folio")
    private Instruccion folio;

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @Column(name = "ruta_edo_cta")
    private String rutaEdoCta;

}
