package com.bim.seif.services;

import com.bim.seif.models.Cliente;
import com.bim.seif.repositories.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    /**
     * Implementación del método principal de Spring Security para cargar detalles de usuario.
     * * @param email Email del cliente.
     * @return UserDetails (Cliente).
     * @throws UsernameNotFoundException si el usuario no existe o está inactivo.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Intentando cargar detalles del usuario para el email: {}", email);
        try {
            return customerRepository.findByEmailAndFechaBajaIsNull(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado o inactivo: {}", email);
                    return new UsernameNotFoundException("User not found");
                });
        } catch (UsernameNotFoundException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Fallo en la consulta a DB para el usuario: {}", email, e);
            throw new RuntimeException("Error interno al buscar el usuario.", e);
        }
    }

    /**
     * Busca un cliente por su email, independientemente de su estado de baja.
     * * @param email Email del cliente.
     * @return Cliente.
     * @throws UsernameNotFoundException si el usuario no existe.
     */
    public Cliente getUserByEmail(String email) {
        log.debug("Buscando cliente por email: {}", email);
        try {
            return customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado por email: {}", email);
                    return new UsernameNotFoundException("User not found");
                });
        } catch (UsernameNotFoundException e) {
            throw e; 
        } catch (Exception e) {
            log.error("Fallo en la consulta a DB para el usuario: {}", email, e);
            throw new RuntimeException("Error interno al buscar el cliente por email.", e);
        }
    }

    /**
     * Guarda o actualiza un cliente.
     * * @param user El objeto Cliente a guardar.
     * @return El objeto Cliente guardado.
     */
    public Cliente save(Cliente user) {
        log.info("Solicitud para guardar/actualizar cliente con email: {}", user.getEmail());
        try {
            Cliente savedUser = customerRepository.save(user);
            log.info("Cliente con ID {} y email {} guardado/actualizado exitosamente.", savedUser.getId(), savedUser.getEmail());
            return savedUser;
        } catch (Exception e) {
            log.error("Fallo al intentar guardar el cliente con email: {}", user.getEmail(), e);
            throw new RuntimeException("Error al guardar el cliente.", e);
        }
    }

}