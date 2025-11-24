package com.bim.seif.models.mappers;

import com.bim.seif.models.Instruccion;
import com.bim.seif.models.InstruccionJuridica;
import com.bim.seif.models.dto.InstruccionDto;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(uses = { OperacionJuridicaMapper.class }, nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface InstruccionMapper {

    InstruccionMapper INSTANCE = Mappers.getMapper(InstruccionMapper.class);

    InstruccionDto instruccionToInstruccionDTO(Instruccion instruccion);

    List<InstruccionJuridicaDto> instruccionJuridicaToInstruccionJuridicaDTOList(
            List<InstruccionJuridicaDto> instrucciones);

    InstruccionJuridicaDto instruccionJuridicaToInstruccionJuridicaDTO(InstruccionJuridica instruccion);

    List<InstruccionJuridicaDto> instruccionJuridicaToInstruccionJuridicaDTOListTwo(
            List<InstruccionJuridica> instrucciones);

}