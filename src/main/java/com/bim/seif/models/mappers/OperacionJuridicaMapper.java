package com.bim.seif.models.mappers;

import com.bim.seif.models.OperacionJuridica;
import com.bim.seif.models.dto.OperacionJuridicaDto;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = {InstruccionMapper.class})
public interface OperacionJuridicaMapper {

    OperacionJuridicaMapper INSTANCE = Mappers.getMapper(OperacionJuridicaMapper.class);

    @Mapping(target = "estatusCve", source = "estatus.cve")
    @Named("sinInstruccion")
    @Mapping(target = "instruccion", ignore = true)
    OperacionJuridicaDto toDtoSinInstruccion(OperacionJuridica operacionJuridica);

    @IterableMapping(qualifiedByName = "sinInstruccion")
    List<OperacionJuridicaDto> toDto(List<OperacionJuridica> operacionJuridica);

//    @Mapping(target="instruccion.estatus.cve", source = "instruccion.estatus")
//    OperacionJuridica toEntity(OperacionJuridicaDto operacionJuridicaDto);
//    List<OperacionJuridica> toEntity(List<OperacionJuridicaDto> operacionJuridicaDto);

}
