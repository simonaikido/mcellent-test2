package com.bim.seif.repositories;

import com.bim.seif.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
//import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Cliente, Long> {

   //User findByName(String name);
   Optional<Cliente> findBynombreAndPassword(String nombre, String password);

}
