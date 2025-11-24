package com.bim.seif.models;


import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "instruccion")
public class Instruccion {

    @Id
    private String folio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fideicomiso_folio")
    private Fideicomiso fideicomiso;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    private String comentario;

    @Column(name = "cliente_carga")
    private String clienteCarga;
    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaAtencion;
    
    private LocalDateTime fechaRechazo;
    private LocalDateTime fechaCancelacion;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

    @OneToMany(mappedBy = "instruccion", fetch = FetchType.LAZY)
    private List<ComentarioInstruccion> comentarios;

    public static Object size() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }
}
