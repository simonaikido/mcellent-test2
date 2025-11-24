package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "solicitud_documento_adicional")
public class ArchivoAdicional {


    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    private LocalDateTime fecha_carga;
    private LocalDateTime fecha_solicitud;

    @Column(name = "nombre")
    private String nombreArchivo;
    private String ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folio")
    private Instruccion instruccion;


}
