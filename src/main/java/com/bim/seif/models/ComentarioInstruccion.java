package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "COMENTARIO_INSTRUCCION")
public class ComentarioInstruccion {

    @Id
    private long id;

    private String comentario;
    private LocalDateTime fecha_comentario;
    private boolean leeida;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instruccion_folio")
    private Instruccion instruccion;

}
