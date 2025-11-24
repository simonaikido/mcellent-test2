package com.bim.seif.services;


import com.bim.seif.models.ArchivoAdicional;
import com.bim.seif.models.DocumentoFideicomiso;
import com.bim.seif.repositories.ArchivoAdicionalRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ArchivoAdicionalService {

    @Autowired
    private ArchivoAdicionalRepository archivoAdicionalRepository;


    @Transactional
    public ArchivoAdicional guardarArchivoAdicional(ArchivoAdicional archivoAdicional) {
        return archivoAdicionalRepository.save(archivoAdicional);
    }

    @Transactional(readOnly = true)
    public Optional<List<ArchivoAdicional>> obtenerPorInstruccion(String folio) {
        return archivoAdicionalRepository.findByInstruccionFolio(folio);
    }

}
