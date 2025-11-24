package com.bim.seif.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bim.seif.models.ClienteAudit;

@Repository
public interface ClienteAuditRepository extends JpaRepository<ClienteAudit, Long>{
}
