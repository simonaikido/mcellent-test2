package com.bim.seif.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
public class Fideicomiso {

    @Id
    private String folio;
    @ManyToOne
    private Region region;
    private String alias;
    private String nombreCliente;
    private boolean bloqueado;
}
