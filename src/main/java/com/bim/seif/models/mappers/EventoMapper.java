package com.bim.seif.models.mappers;

import com.bim.seif.dto.EventoDto;
import com.bim.seif.models.Evento;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface EventoMapper {
    Evento toEntity(EventoDto eventoDto);

    EventoDto toDto(Evento evento);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Evento partialUpdate(EventoDto eventoDto, @MappingTarget Evento evento);
}