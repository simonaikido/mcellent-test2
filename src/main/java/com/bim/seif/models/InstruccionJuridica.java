package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "INSTRUCCION_JURIDICA")
//@PrimaryKeyJoinColumn(name="instruccion_folio")
public class InstruccionJuridica { // extends Instruccion

    @Id
    private String folio;

    @OneToOne
    @MapsId
    @JoinColumn(name = "folio")
    private Instruccion instruccion;

    private String responsable;

    private boolean  urgente;
    private boolean operadaParcialmente;
    private boolean solicitudCorreccion;

    @ManyToOne
    @JoinColumn(name = "estatus_cve")
    private EstatusInstruccion estatus;
    private Timestamp fechaModificacion;

    @OneToMany(mappedBy = "instruccion", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<OperacionJuridica> operaciones;
}
