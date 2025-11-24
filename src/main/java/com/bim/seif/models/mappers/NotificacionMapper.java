package com.bim.seif.models.mappers;

import com.bim.seif.dto.NotificacionDto;
import com.bim.seif.models.Notificacion;
import org.mapstruct.*;


@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificacionMapper {

    NotificacionDto toDto(Notificacion notificacion);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Notificacion partialUpdate(NotificacionDto notificacionDto, @MappingTarget Notificacion notificacion);
}