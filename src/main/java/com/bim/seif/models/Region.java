package com.bim.seif.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Region {

    @Id
    private String cve;
    private String descripcion;
}
