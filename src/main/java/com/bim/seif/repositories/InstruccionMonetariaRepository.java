package com.bim.seif.repositories;


import com.bim.seif.models.InstruccionMonetaria;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstruccionMonetariaRepository extends JpaRepository<InstruccionMonetaria, String> {
    Optional<InstruccionMonetaria> findByInstruccionFolio(String folio);
}
