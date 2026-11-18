package com.covoiturage.covoiturage.mapper.reservation;

import com.covoiturage.covoiturage.dto.reservation.ReservationResponse;
import com.covoiturage.covoiturage.model.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    @Mapping(source = "passager.id", target = "passagerId")
    @Mapping(source = "trajet.id", target = "trajetId")
    @Mapping(source = "pointMontee.ville", target = "villeMontee")
    @Mapping(source = "pointDescente.ville", target = "villeDescente")
    ReservationResponse toResponse(Reservation reservation);
}