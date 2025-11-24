package com.bim.seif.models;

import java.util.HashSet;
import java.util.Set;

public enum TipoEvento {

        cuenta_alta("ALTA DE CUENTA", Set.of(
                        TipoPropiedad.cuenta, TipoPropiedad.fideicomiso)),
        cuenta_solicitud("SOLICITUD DE ESTADO DE CUENTA", Set.of(
                        TipoPropiedad.cuenta, TipoPropiedad.fideicomiso)),
        divisa_confirmacion("Alta decuenta", Set.of(
                        TipoPropiedad.divisa, TipoPropiedad.fideicomiso, TipoPropiedad.instruccion)),
        instruccion_ejecucion("INSTRUCCION FINALIZADA", Set.of(
                        TipoPropiedad.fideicomiso, TipoPropiedad.instruccion)),
        instruccion_recepcion("RECEPCION DE INSTRUCCION", Set.of(
                        TipoPropiedad.fideicomiso, TipoPropiedad.instruccion)),
        instruccion_adicional("SOLICITUD DE DOCUMENTOS ADICIONALES", Set.of(
                        TipoPropiedad.fideicomiso, TipoPropiedad.instruccion)),
        instruccion_rechazo("INSTRUCCION RECHAZADA", Set.of(
                        TipoPropiedad.fideicomiso, TipoPropiedad.instruccion)),
        pld_positivo("PLD POSITIVO", new HashSet<>()), seguridad_token_cliente("ENVIO OTP CLIENTE", Set.of(
                        TipoPropiedad.cliente, TipoPropiedad.seguridad)),
        bienvenida("ALTA DE USUARIO", Set.of(
                        TipoPropiedad.cliente));

        private final Set<TipoPropiedad> variables;
        private final String descripcion;

        TipoEvento(String descripcion, Set<TipoPropiedad> variables) {
                this.variables = variables;
                this.descripcion = descripcion;
        }

        public Set<TipoPropiedad> getVariables() {
                if (this.variables == null) {
                        return new HashSet<>();
                }
                return variables;
        }

        public String getDescripcion() {
                return descripcion;
        }

}
