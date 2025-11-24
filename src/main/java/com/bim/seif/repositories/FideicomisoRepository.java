package com.bim.seif.repositories;


import com.bim.seif.models.Fideicomiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FideicomisoRepository extends JpaRepository<Fideicomiso, String> {

Fideicomiso findByFolio(String folio);
}
