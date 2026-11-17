package com.covoiturage.covoiturage.mapper.trajet;


import com.covoiturage.covoiturage.dto.trajet.PointArretResponse;
import com.covoiturage.covoiturage.model.PointArret;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointArretMapper{
    PointArretResponse toResponse(PointArret pointArret);
}
