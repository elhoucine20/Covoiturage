package com.covoiturage.covoiturage.controller;

import com.covoiturage.covoiturage.dto.reservation.ReservationRequest;
import com.covoiturage.covoiturage.dto.reservation.ReservationResponse;
import com.covoiturage.covoiturage.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserver(@Valid @RequestBody ReservationRequest request){
        return reservationService.reserver(request);
    }


    @PatchMapping("/reservations/{id}/annulation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void annuler(@PathVariable Long id,@RequestParam Long passagerId){
        reservationService.annuler(id,passagerId);
    }

    @GetMapping("/reservations")
    public List<ReservationResponse> listerParPassager(@RequestParam Long passagerId){
        return reservationService.listerParPassager(passagerId);
    }

    @GetMapping("/trajets/{id}/reservations")
    public List<ReservationResponse> listerParTrajet(@PathVariable Long trajetId,@RequestParam Long conducteurId){
        return reservationService.listerParTrajet(trajetId,conducteurId);
    }

}
