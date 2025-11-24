package com.bim.seif.repositories;
import com.bim.seif.models.ComentarioInstruccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioInstruccionRepository extends JpaRepository<ComentarioInstruccion, Long> {

    // Buscar comentarios por folio de instrucción
    List<ComentarioInstruccion> findByInstruccion_Folio(String folioInstruccion);

    // Buscar comentarios no leídos
    List<ComentarioInstruccion> findByLeeidaFalse();

    // Buscar comentarios por instrucción y estado de lectura
    List<ComentarioInstruccion> findByInstruccion_FolioAndLeeida(String folioInstruccion, boolean leeida);
}