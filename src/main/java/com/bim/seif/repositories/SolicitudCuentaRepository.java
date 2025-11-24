package com.bim.seif.repositories;

import com.bim.seif.models.SolicitudCuenta;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudCuentaRepository extends JpaRepository<SolicitudCuenta, Long> {
    @Query("select sc from SolicitudCuenta sc where sc.folio.folio = :folio")
    Optional<SolicitudCuenta> findByInstruccionFolio(@Param("folio") String folio);
}
