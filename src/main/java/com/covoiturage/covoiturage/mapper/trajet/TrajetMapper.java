package com.covoiturage.covoiturage.mapper.trajet;

import com.covoiturage.covoiturage.dto.trajet.TrajetRechercheResponse;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.mapper.conducteur.ConducteurMapper;
import com.covoiturage.covoiturage.mapper.vehicule.VehiculeMapper;
import com.covoiturage.covoiturage.model.Trajet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {ConducteurMapper.class, VehiculeMapper.class, PointArretMapper.class})
public interface TrajetMapper {


    @Mapping(source = "trajet.id", target = "id")
    @Mapping(source = "placesRestantes", target = "placesRestantes")
    TrajetResponse toResponse(Trajet trajet, int placesRestantes);

    //TrajetRechercheResponse toResponse(int placesRestantes , Trajet trajet);
}
