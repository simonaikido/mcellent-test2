package com.bim.seif.services;

import com.bim.seif.models.DocumentoFideicomiso;
import com.bim.seif.repositories.DocumentoFideicomisoRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DocumentoFideicomisoService {

    private final DocumentoFideicomisoRepository documentoFideicomisoRepository;

    @Autowired
    public DocumentoFideicomisoService(DocumentoFideicomisoRepository documentoFideicomisoRepository) {
        this.documentoFideicomisoRepository = documentoFideicomisoRepository;
        log.info("DocumentoFideicomisoService inicializado.");
    }

    /**
     * Guarda un nuevo documento asociado a un fideicomiso (e.g., estado de cuenta).
     * @param documentoFideicomiso El objeto DocumentoFideicomiso a persistir.
     * @return El objeto DocumentoFideicomiso guardado.
     */
    @Transactional
    public DocumentoFideicomiso guardarDocumentoFideicomiso(DocumentoFideicomiso documentoFideicomiso) {
        log.info("Persistiendo documento de tipo '{}' para el fideicomiso: {}.", 
                 documentoFideicomiso.getTipoArchivo(), documentoFideicomiso.getFideicomisoFolio());
        log.debug("Detalle del archivo: Nombre='{}', Ruta='{}'", 
                  documentoFideicomiso.getNombre(), documentoFideicomiso.getRuta());
        
        try {
            DocumentoFideicomiso savedDoc = documentoFideicomisoRepository.save(documentoFideicomiso);
            log.info("Documento guardado exitosamente. ID interno: {}.", savedDoc.getId());
            return savedDoc;
        } catch (Exception e) {
            log.error("Fallo al guardar el documento para el folio {}.", 
                      documentoFideicomiso.getFideicomisoFolio(), e);
            throw new RuntimeException("Error al guardar el documento del fideicomiso.", e);
        }
    }


    /**
     * Obtiene todos los documentos asociados a un fideicomiso específico.
     * @param folio Folio del fideicomiso.
     * @return Lista de DocumentoFideicomiso.
     */
    @Transactional(readOnly = true)
    public List<DocumentoFideicomiso> obtenerPorFideicomiso(String folio) {
        log.info("Buscando documentos para el fideicomiso con folio: {}.", folio);
        
        try {
            List<DocumentoFideicomiso> documentos = documentoFideicomisoRepository.findByFideicomisoFolio(folio);
            
            if (documentos.isEmpty()) {
                log.info("No se encontraron documentos para el fideicomiso {}.", folio);
            } else {
                log.info("Encontrados {} documentos para el fideicomiso {}.", documentos.size(), folio);
            }
            
            return documentos;
        } catch (Exception e) {
            log.error("Fallo al obtener documentos para el folio {}.", folio, e);
            throw new RuntimeException("Error al consultar documentos del fideicomiso.", e);
        }
    }

    public List<DocumentoFideicomiso> getDocumentosByFolioFideicomiso(String fideicomisoFolio) {
        return documentoFideicomisoRepository.findByFideicomisoFolio(fideicomisoFolio);
    }
}