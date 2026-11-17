package com.covoiturage.covoiturage.controller;


import com.covoiturage.covoiturage.dto.trajet.TrajetRequest;
import com.covoiturage.covoiturage.dto.trajet.TrajetResponse;
import com.covoiturage.covoiturage.service.TrajetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

}
