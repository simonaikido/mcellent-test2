package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class OperacionJuridica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaRevision;

    @ManyToOne
    @JoinColumn(name="instruccion_folio")
    private InstruccionJuridica instruccion;

    @ManyToOne
    @JoinColumn(name = "tipo_operacion_cve")
    private TipoOperacionJuridica tipoOperacionJuridica;

    private String descripcionOperacion;
    private String comentario;
    private String observaciones;
    private String clienteCorreoElectronico;

    private boolean instruccionCumpleFines;
    private boolean boolClienteEmail;
    private boolean firmasCorrectas;
    private Boolean aprobada;
    private Boolean omitida;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "estatus_cve")
    private EstatusOperacion estatus;
    @ManyToOne
    @JoinColumn(name = "id_comprobante", nullable = true)   // columna en BD
    private ComprobanteOperacion comprobante;
}
