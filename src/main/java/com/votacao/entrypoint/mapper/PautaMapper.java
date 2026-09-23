package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Pauta;
import com.votacao.entrypoint.api.dto.PautaRequestDTO;
import com.votacao.entrypoint.api.dto.PautaResponseDTO;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PautaMapper {

    PautaEntity toEntity(Pauta pauta);

    Pauta toDomain(PautaEntity entity);

    @Mapping(target = "id", ignore = true)
    Pauta toDomain(PautaRequestDTO request);

    PautaResponseDTO toResponse(Pauta pauta);

    List<PautaResponseDTO> toResponseList(List<Pauta> pautas);

}