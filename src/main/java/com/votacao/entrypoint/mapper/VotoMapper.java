package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Voto;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = SessaoMapper.class)
public interface VotoMapper {

    Voto toDomain(VotoEntity entity);

    VotoEntity toEntity(Voto voto);

}