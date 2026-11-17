package com.covoiturage.covoiturage.mapper.passager;

import com.covoiturage.covoiturage.dto.passager.PassagerRequest;
import com.covoiturage.covoiturage.dto.passager.PassagerResponse;
import com.covoiturage.covoiturage.model.Passager;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PassagerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "salt", ignore = true)
    Passager toEntity(PassagerRequest request);

    PassagerResponse toResponse(Passager passager);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "salt", ignore = true)
    void updateEntity(PassagerRequest request, @MappingTarget Passager passager);
}