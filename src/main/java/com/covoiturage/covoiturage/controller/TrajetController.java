package com.covoiturage.covoiturage.controller;


import com.covoiturage.covoiturage.dto.trajet.TrajetRechercheResponse;
import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.service.TrajetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/trajets")
@RequiredArgsConstructor
public class TrajetController {

    private final TrajetService trajetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrajetResponse publier(@Valid @RequestBody TrajetRequest request) {
        return trajetService.publier(request);
    }

    @GetMapping("/{id}")
    public TrajetResponse trouverParId(@PathVariable Long id) {
        return trajetService.trouverParId(id);
    }


    @GetMapping("/recherche")
    public Page<TrajetRechercheResponse> rechercher(@RequestParam String villeDepart, @RequestParam String villeArrivee,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date,
                                                    @RequestParam(required = false) BigDecimal prixMax,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size){
        return trajetService.rechercher(villeDepart,villeArrivee,date,prixMax,page,size);
    }

}
