package com.bim.seif.repositories;

import com.bim.seif.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Cliente,Long> {

    Optional<Cliente> findByEmailAndFechaBajaIsNull(String correo);

    Optional<Cliente> findByEmail(String email);
}
