package com.bim.seif.repositories;

import com.bim.seif.models.Instruccion;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface InstruccionRepository extends JpaRepository<Instruccion, String> {
    int countByFideicomisoFolio(String folio);

    List<Instruccion> findAllByClienteCarga(String folio);

    Instruccion findByFolio(String folio);

    @Query(value = """
            SELECT DISTINCT
                rutaArchivo,
                folioMonetaria,
                fideicomisoFolio,
                fideicomisoAlias,
                fecha_aprobacion,
                fechaAlta,
                folioInstruccion,
                fecha_cancelacion,
                estatus_cve,
                clienteCarga,
                documentos,
                fechasDocumentos,
                comentarios,
                comentariosFechaInstruccion,
                programada,
                rutaEdoCta,
                tipoInstruccionTxt,
                isMonetaria,
                fechaRechazo
            FROM (
                  /* 1) Con instruccion_monetaria (puede PROG) */
                  (
                      SELECT
                          i.ruta_archivo as rutaArchivo,
                          COALESCE(i.folio, '') as folioMonetaria,
                          i.fideicomiso_folio as fideicomisoFolio,
                          f.alias as fideicomisoAlias,
                          CASE WHEN i.fecha_aprobacion IS NULL THEN ''
                               ELSE FORMAT(i.fecha_aprobacion, 'yyyy-MM-dd')
                          END as fecha_aprobacion,
                          CASE WHEN i.fecha_alta IS NULL THEN ''
                               ELSE FORMAT(i.fecha_alta, 'yyyy-MM-dd')
                          END as fechaAlta,
                          i.folio as folioInstruccion,
                          CASE WHEN i.fecha_cancelacion IS NULL THEN FORMAT(sq.fecha_solicitud, 'yyyy-MM-dd')
                               ELSE FORMAT(i.fecha_cancelacion, 'yyyy-MM-dd')
                          END as fecha_cancelacion,

                          /*ESTATUS UNIFICADO (Monetaria)*/
                          CASE
                              WHEN i.fecha_cancelacion IS NOT NULL                                 THEN 'CA'
                              WHEN i.fecha_rechazo    IS NOT NULL OR im.estatus_cve = 'RE'         THEN 'RE'
                              WHEN i.fecha_aprobacion IS NOT NULL OR im.estatus_cve = 'FI'         THEN 'FI'
                              WHEN i.fecha_atencion   IS NOT NULL                                   THEN 'PR'
                              WHEN im.programada = 1 AND ip.cuenta_cargo IS NOT NULL               THEN 'PROG'
                              WHEN NULLIF(LTRIM(RTRIM(im.estatus_cve)), '') IS NOT NULL            THEN im.estatus_cve
                              ELSE 'PR'
                          END as estatus_cve,

                          i.cliente_carga as clienteCarga,
                          (SELECT STRING_AGG(sda.nombre, '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as documentos,
                          (SELECT STRING_AGG(FORMAT(sda.fecha_solicitud, 'yyyy-MM-dd'), '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as fechasDocumentos,
                          (SELECT STRING_AGG(ci.comentario, '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentarios,
                          (SELECT STRING_AGG(FORMAT(ci.fecha_comentario, 'yyyy-MM-dd'), '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentariosFechaInstruccion,
                          im.programada as programada,
                          sq.ruta_edo_cta as rutaEdoCta,
                          'Monetaria' as tipoInstruccionTxt,
                          CAST(1 AS INT) as isMonetaria,
                          CASE WHEN i.fecha_rechazo IS NULL THEN ''
                               ELSE FORMAT(i.fecha_rechazo, 'yyyy-MM-dd')
                          END AS fechaRechazo
                      FROM instruccion i
                      INNER JOIN instruccion_monetaria im ON im.folio = i.folio
                      OUTER APPLY (
                          SELECT TOP 1 sc.fecha_solicitud, sc.ruta_edo_cta
                          FROM solicitud_cuenta sc
                          WHERE sc.instruccion_folio = i.folio
                          ORDER BY sc.fecha_solicitud DESC
                      ) AS sq
                      LEFT JOIN fideicomiso f ON f.folio = i.fideicomiso_folio
                      LEFT JOIN instruccion_programada ip ON i.folio = ip.folio
                      WHERE i.cliente_carga = :clienteEmail
                      GROUP BY
                          i.folio,
                          i.fideicomiso_folio,
                          i.ruta_archivo,
                          f.alias,
                          i.fecha_aprobacion,
                          i.fecha_alta,
                          i.fecha_cancelacion,
                          sq.fecha_solicitud,
                          i.fecha_rechazo,
                          i.cliente_carga,
                          i.fecha_atencion,
                          im.programada,
                          ip.cuenta_cargo,
                          sq.ruta_edo_cta,
                          im.estatus_cve
                  )

                  UNION ALL

                  /* 2) Con instruccion_juridica (NO monetaria) */
                  (
                      SELECT
                          i.ruta_archivo as rutaArchivo,
                          i.folio as folioMonetaria,
                          i.fideicomiso_folio as fideicomisoFolio,
                          f.alias as fideicomisoAlias,
                          CASE WHEN i.fecha_aprobacion IS NULL THEN ''
                               ELSE FORMAT(i.fecha_aprobacion, 'yyyy-MM-dd')
                          END as fecha_aprobacion,
                          CASE WHEN i.fecha_alta IS NULL THEN ''
                               ELSE FORMAT(i.fecha_alta, 'yyyy-MM-dd')
                          END as fechaAlta,
                          i.folio as folioInstruccion,
                          CASE WHEN i.fecha_cancelacion IS NULL THEN ''
                               ELSE FORMAT(i.fecha_cancelacion, 'yyyy-MM-dd')
                          END as fecha_cancelacion,

                          /* (Jurídica)
                             Prioridad: CA > RE > FI > PR > (estado subtabla) > PR
                             (Incluimos ij.estatus_cve)
                          */
                          CASE
                              WHEN i.fecha_cancelacion IS NOT NULL                                 THEN 'CA'
                              WHEN i.fecha_rechazo    IS NOT NULL OR ij.estatus_cve = 'RE'         THEN 'RE'
                              WHEN i.fecha_aprobacion IS NOT NULL OR ij.estatus_cve = 'FI'         THEN 'FI'
                              WHEN i.fecha_atencion   IS NOT NULL                                   THEN 'PR'
                              WHEN NULLIF(LTRIM(RTRIM(ij.estatus_cve)), '') IS NOT NULL            THEN ij.estatus_cve
                              ELSE 'PR'
                          END as estatus_cve,

                          i.cliente_carga as clienteCarga,
                          (SELECT STRING_AGG(sda.nombre, '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as documentos,
                          (SELECT STRING_AGG(FORMAT(sda.fecha_solicitud, 'yyyy-MM-dd'), '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as fechasDocumentos,
                          (SELECT STRING_AGG(ci.comentario, '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentarios,
                          (SELECT STRING_AGG(FORMAT(ci.fecha_comentario, 'yyyy-MM-dd'), '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentariosFechaInstruccion,
                          CAST(0 AS INT) as programada,
                          '' as rutaEdoCta,
                          'No Monetaria' as tipoInstruccionTxt,
                          CAST(0 AS INT) as isMonetaria,
                          CASE WHEN i.fecha_rechazo IS NULL THEN ''
                               ELSE FORMAT(i.fecha_rechazo, 'yyyy-MM-dd')
                          END AS fechaRechazo
                      FROM instruccion i
                      INNER JOIN instruccion_juridica ij ON ij.folio = i.folio
                      LEFT JOIN fideicomiso f ON f.folio = i.fideicomiso_folio
                      WHERE i.cliente_carga = :clienteEmail
                  )

                  UNION ALL

                  /* 3) Resto: sin monetaria NI jurídica => Sin clasificar */
                  (
                      SELECT
                          i.ruta_archivo as rutaArchivo,
                          i.folio as folioMonetaria,
                          i.fideicomiso_folio as fideicomisoFolio,
                          f.alias as fideicomisoAlias,
                          CASE WHEN i.fecha_aprobacion IS NULL THEN ''
                               ELSE FORMAT(i.fecha_aprobacion, 'yyyy-MM-dd')
                          END as fecha_aprobacion,
                          CASE WHEN i.fecha_alta IS NULL THEN ''
                               ELSE FORMAT(i.fecha_alta, 'yyyy-MM-dd')
                          END as fechaAlta,
                          i.folio as folioInstruccion,
                          CASE WHEN i.fecha_cancelacion IS NULL THEN ''
                               ELSE FORMAT(i.fecha_cancelacion, 'yyyy-MM-dd')
                          END as fecha_cancelacion,

                          /*Sin clasificar*/
                          CASE
                              WHEN i.fecha_cancelacion IS NOT NULL THEN 'CA'
                              WHEN i.fecha_rechazo    IS NOT NULL THEN 'RE'
                              WHEN i.fecha_aprobacion IS NOT NULL THEN 'FI'
                              WHEN i.fecha_atencion   IS NOT NULL THEN 'PR'
                              ELSE 'PR'
                          END as estatus_cve,

                          i.cliente_carga as clienteCarga,
                          (SELECT STRING_AGG(sda.nombre, '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as documentos,
                          (SELECT STRING_AGG(FORMAT(sda.fecha_solicitud, 'yyyy-MM-dd'), '||')
                             FROM solicitud_documento_adicional sda
                            WHERE sda.instruccion_folio = i.folio) as fechasDocumentos,
                          (SELECT STRING_AGG(ci.comentario, '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentarios,
                          (SELECT STRING_AGG(FORMAT(ci.fecha_comentario, 'yyyy-MM-dd'), '||')
                             FROM comentario_instruccion ci
                            WHERE ci.instruccion_folio = i.folio AND ci.tipo_comentario IN ('observacion','rechazo')) as comentariosFechaInstruccion,
                          CAST(0 AS INT) as programada,
                          '' as rutaEdoCta,
                          'Sin clasificar' as tipoInstruccionTxt,
                          CAST(0 AS INT) as isMonetaria,
                          CASE WHEN i.fecha_rechazo IS NULL THEN ''
                               ELSE FORMAT(i.fecha_rechazo, 'yyyy-MM-dd')
                          END AS fechaRechazo
                      FROM instruccion i
                      LEFT JOIN fideicomiso f ON f.folio = i.fideicomiso_folio
                      WHERE i.cliente_carga = :clienteEmail
                        AND NOT EXISTS (SELECT 1 FROM instruccion_monetaria im WHERE im.folio = i.folio)
                        AND NOT EXISTS (SELECT 1 FROM instruccion_juridica  ij WHERE ij.folio = i.folio)
                  )
            ) AS u
            """, nativeQuery = true)
    List<Object[]> findInstruccionesCompletasByCliente(@Param("clienteEmail") String clienteEmail);

    @Query(value = "SELECT ip.cuenta_cargo, ip.cuenta_abono_cuenta, ip.monto, FORMAT(p.fecha_ejecucion, 'yyyy-MM-dd') FROM programacion p LEFT JOIN instruccion_programada ip "
            +
            "ON p.instruccion_progamada_folio = ip.folio " +
            "WHERE ip.folio = :folioInstruccion ", nativeQuery = true)
    List<Object[]> findFechasInstruccionProgramada(@Param("folioInstruccion") String folioInstruccion);

    @Query(value = """
                SELECT
                    ij.folio,
                    ij.responsable,
                    ij.estatus_cve as estatusCve,
                    ij.solicitud_correccion as solicitudCorreccion,
                    i.fecha_alta,
                    f.alias,
                    i.ruta_archivo,
                    i.comentario
                FROM instruccion_juridica ij
                LEFT JOIN instruccion i ON i.folio = ij.folio
                LEFT JOIN fideicomiso f ON i.fideicomiso_folio = f.folio
                WHERE ij.estatus_cve = 'PE' AND i.cliente_carga = :user
            """, nativeQuery = true)
    Optional<List<Map<String, Object>>> findBuscarInstruccionesSolicitudes(@Param("user") String user);

    @Query(value = """
                SELECT
                    oj.id,
                    oj.tipo_operacion_cve,
                    oj.estatus_cve,
                    oj.descripcion_operacion,
                    oj.comentario,
                    oj.observaciones,
                    toj.descripcion
                FROM operacion_juridica oj
                JOIN TIPO_OPERACION_JURIDICA toj ON toj.cve = oj.tipo_operacion_cve
                WHERE oj.instruccion_folio = :folioInstruccion
            """, nativeQuery = true)
    Optional<List<Map<String, Object>>> obtenerOperacionesJuridicas(@Param("folioInstruccion") String folioInstruccion);

    @Query(value = """
            SELECT
                s.id,
                s.id_operacion,
                s.fecha_solicitud,
                s.fecha_carga,
                s.nombre_archivo,
                s.ruta_archivo,
                s.descripcion_del_acto,
                s.nombre_formato,
                s.ruta_formato,
                s.nota
            FROM solicitud_documento_operacion_juridica s
            WHERE id_operacion = :id
            """, nativeQuery = true)
    Optional<List<Map<String, Object>>> obtenerArchivosSolicitadosPorOperacion(@Param("id") Long id);

    @Query(value = """
                DECLARE @FolioInstruccion VARCHAR(50) = :idFolio;

                BEGIN TRANSACTION

            -- Actualizar las tres tablas en secuencia
                UPDATE ij SET [solicitud_correccion] = 0 , [estatus_cve] = 'PR'
                FROM [dbSEIF].[dbo].[instruccion_juridica] ij
                WHERE ij.folio = @FolioInstruccion;

                UPDATE oj SET [estatus_cve] = 'PE'
                FROM [dbSEIF].[dbo].[operacion_juridica] oj
                WHERE oj.instruccion_folio = @FolioInstruccion;

                UPDATE sdoj SET
                        ruta_archivo = 'charlymendez2400+cliente01@gmail.com/85101771/instrucciones/85101771000023/6/FI-FSO-FID-E-09 KYC Persona Moral V.3 200524 (juna).xlsm',
                        fecha_carga = '2025-08-29'
                FROM [dbSEIF].[dbo].[solicitud_documento_operacion_juridica] sdoj
                INNER JOIN [dbSEIF].[dbo].[operacion_juridica] oj ON sdoj.id_operacion = oj.id
                WHERE oj.instruccion_folio = @FolioInstruccion;

            -- Retornar todos los datos actualizados
                        SELECT
                ij.*,
                oj.*,
                sdoj.*
                FROM [dbSEIF].[dbo].[instruccion_juridica] ij
                LEFT JOIN [dbSEIF].[dbo].[operacion_juridica] oj ON ij.folio = oj.instruccion_folio
                LEFT JOIN [dbSEIF].[dbo].[solicitud_documento_operacion_juridica] sdoj ON oj.id = sdoj.id_operacion
                WHERE ij.folio = @FolioInstruccion;

                COMMIT TRANSACTION;
                """, nativeQuery = true)
    void setearInstruccion(@Param("idFolio") Long idFolio);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE [dbSEIF].[dbo].[instruccion_juridica]
            SET [solicitud_correccion] = 0, [estatus_cve] = 'PR'
            WHERE folio = :idFolio
            """, nativeQuery = true)
    int updateInstruccionJuridica(@Param("idFolio") Long idFolio);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE [dbSEIF].[dbo].[operacion_juridica]
            SET [estatus_cve] = 'PE'
            WHERE instruccion_folio = :idFolio
            """, nativeQuery = true)
    int updateOperacionJuridica(@Param("idFolio") Long idFolio);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE sdoj
            SET sdoj.ruta_archivo = :rutaArchivo,
                sdoj.fecha_carga = :fechaCarga
            FROM [dbSEIF].[dbo].[solicitud_documento_operacion_juridica] sdoj
            INNER JOIN [dbSEIF].[dbo].[operacion_juridica] oj ON sdoj.id_operacion = oj.id
            WHERE oj.instruccion_folio = :idFolio
              AND LOWER(
                    CASE
                      WHEN CHARINDEX('.', sdoj.nombre_archivo) > 0
                        THEN LEFT(sdoj.nombre_archivo, LEN(sdoj.nombre_archivo) - CHARINDEX('.', REVERSE(sdoj.nombre_archivo)+'.'))
                      ELSE sdoj.nombre_archivo
                    END
                  ) = LOWER(
                    CASE
                      WHEN CHARINDEX('.', :nombreArchivo) > 0
                        THEN LEFT(:nombreArchivo, LEN(:nombreArchivo) - CHARINDEX('.', REVERSE(:nombreArchivo)+'.'))
                      ELSE :nombreArchivo
                    END
                  )
            """, nativeQuery = true)
    int updateSolicitudDocumentoRuta(
            @Param("idFolio") Long idFolio,
            @Param("nombreArchivo") String nombreArchivo,
            @Param("rutaArchivo") String rutaArchivo,
            @Param("fechaCarga") LocalDate fechaCarga);

}
