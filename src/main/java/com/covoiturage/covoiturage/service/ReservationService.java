package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.reservation.ReservationRequest;
import com.covoiturage.covoiturage.dto.reservation.ReservationResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse reserver(ReservationRequest request);
    void annuler(Long reservationId, Long passagerId);
    List<ReservationResponse> listerParPassager(Long passagerId);
    List<ReservationResponse> listerParTrajet(Long trajetId, Long conducteurId);

}
