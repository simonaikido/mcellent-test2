package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Evento {

    @Id
    @Enumerated(EnumType.STRING)
    private TipoEvento cve;
    private String nombre;
    private String asunto;
//    @Lob
    @Column
    private String cuerpoCorreo;
    private String rutaFirma;
    @Column(length = 1000)
    private String comentario;
    private boolean desactivado;
}
