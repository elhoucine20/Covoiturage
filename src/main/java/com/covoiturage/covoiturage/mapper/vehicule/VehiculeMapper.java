package com.covoiturage.covoiturage.mapper.vehicule;

import com.covoiturage.covoiturage.dto.vehicule.VehiculeRequest;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;
import com.covoiturage.covoiturage.model.Vehicule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface VehiculeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    Vehicule toEntity(VehiculeRequest request);

    @Mapping(source = "conducteur.id", target = "conducteurId")
    VehiculeResponse toResponse(Vehicule vehicule);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "conducteur", ignore = true)
    void updateEntity(VehiculeRequest request, @MappingTarget Vehicule vehicule);
}