package com.bim.seif.repositories;

import com.bim.seif.models.DocumentoFideicomiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoFideicomisoRepository extends JpaRepository<DocumentoFideicomiso, Long> {

    // Buscar por ID de fideicomiso
    List<DocumentoFideicomiso> findByFideicomisoFolio(String folio);
    Optional<DocumentoFideicomiso> findByFideicomisoFolioAndNombre(String folio,String nombre);
    
}
