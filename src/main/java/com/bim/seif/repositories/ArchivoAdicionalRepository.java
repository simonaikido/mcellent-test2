
package com.bim.seif.repositories;

import com.bim.seif.models.ArchivoAdicional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchivoAdicionalRepository extends JpaRepository<ArchivoAdicional, Long> {

    // Buscar por ID de fideicomiso
    Optional<List<ArchivoAdicional>> findByInstruccionFolio(String Folio);



}