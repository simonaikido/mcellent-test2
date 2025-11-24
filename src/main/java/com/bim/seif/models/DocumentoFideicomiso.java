package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "documento_fideicomiso")
public class DocumentoFideicomiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @PrePersist
    protected void onCreate() {
        fechaCarga = LocalDateTime.now();
    }

    private String nombre;
    private String ruta;

    @Column(name = "tipo_archivo")
    private String tipoArchivo;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fideicomiso_folio", referencedColumnName = "folio")*/
    private String fideicomisoFolio;

}