package com.bim.seif.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class InstruccionResponse {
    private String archivo;
    private String tipoInstruccion;
    private String nombreFideicomiso;
    private String noFideicomiso;
    private String fechaAprobacion;
    private String fechaAlta;
    private String folio;
    private String fechaCancelacion;
    private String status;
    private String empleadoMail;
    private List<String> listaDocumentos;
    private List<String> fechaNotificacion;
    private List<String> comentariosInstruccion;
    private List<String> comentariosFechaInstruccion;
    private Boolean programada;
    private String rutaEdoCta;
    private String tipoInstruccionTxt;
    private Integer isMonetaria;
    private String fechaRechazo;

    public InstruccionResponse(
            String archivo,
            String tipoInstruccion,
            String nombreFideicomiso,
            String noFideicomiso,
            String fechaAprobacion,
            String fechaAlta,
            String folio,
            String fechaCancelacion,
            String status,
            String empleadoMail,
            String listaDocumentosConcatenados,
            String fechaNotificacionConcatenada,
            String comentariosInstruccionConcatenados,
            String comentariosInstruccionFechaConcatenados,
            Boolean programada,
            String rutaEdoCta,
            String tipoInstruccionTxt,
            Integer isMonetaria
    ) {
        this(archivo, tipoInstruccion, nombreFideicomiso, noFideicomiso, fechaAprobacion, fechaAlta, folio,
             fechaCancelacion, status, empleadoMail, listaDocumentosConcatenados, fechaNotificacionConcatenada,
             comentariosInstruccionConcatenados, comentariosInstruccionFechaConcatenados, programada, rutaEdoCta,
             tipoInstruccionTxt, isMonetaria, null); // fechaRechazo = null
    }

    // === Constructor nuevo (19 params) con fechaRechazo ===
    public InstruccionResponse(
            String archivo,
            String tipoInstruccion,
            String nombreFideicomiso,
            String noFideicomiso,
            String fechaAprobacion,
            String fechaAlta,
            String folio,
            String fechaCancelacion,
            String status,
            String empleadoMail,
            String listaDocumentosConcatenados,
            String fechaNotificacionConcatenada,
            String comentariosInstruccionConcatenados,
            String comentariosInstruccionFechaConcatenados,
            Boolean programada,
            String rutaEdoCta,
            String tipoInstruccionTxt,
            Integer isMonetaria,
            String fechaRechazo
    ) {
        this.archivo = archivo;
        this.tipoInstruccion = tipoInstruccion;
        this.nombreFideicomiso = nombreFideicomiso;
        this.noFideicomiso = noFideicomiso;
        this.fechaAprobacion = fechaAprobacion;
        this.fechaAlta = fechaAlta;
        this.folio = folio;
        this.fechaCancelacion = fechaCancelacion;
        this.status = status;
        this.empleadoMail = empleadoMail;

        this.listaDocumentos = parseList(listaDocumentosConcatenados);
        this.fechaNotificacion = parseList(fechaNotificacionConcatenada);
        this.comentariosInstruccion = parseList(comentariosInstruccionConcatenados);
        this.comentariosFechaInstruccion = parseList(comentariosInstruccionFechaConcatenados);

        this.programada = programada;
        this.rutaEdoCta = rutaEdoCta;
        this.tipoInstruccionTxt = tipoInstruccionTxt;
        this.isMonetaria = isMonetaria;
        this.fechaRechazo = fechaRechazo;
    }

    private List<String> parseList(String concatenated) {
        if (concatenated == null || concatenated.trim().isEmpty()) return new ArrayList<>();
        // Tu query usa '||' como separador
        return Arrays.asList(concatenated.split("\\|\\|"));
    }
}