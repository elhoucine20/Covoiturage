package com.covoiturage.covoiturage.controller;

import com.covoiturage.covoiturage.dto.vehicule.VehiculeRequest;
import com.covoiturage.covoiturage.dto.vehicule.VehiculeResponse;
import com.covoiturage.covoiturage.service.VehiculeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicules")
@RequiredArgsConstructor
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculeResponse ajouter(@Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.ajouter(request);
    }

    @GetMapping("/{id}")
    public VehiculeResponse trouverParId(@PathVariable Long id) {
        return vehiculeService.trouverParId(id);
    }

    @GetMapping
    public List<VehiculeResponse> listerParConducteur(@RequestParam Long conducteurId) {
        return vehiculeService.listerParConducteur(conducteurId);
    }

    @PutMapping("/{id}")
    public VehiculeResponse modifier(@PathVariable Long id,
                                     @Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id, @RequestParam Long conducteurId) {
        vehiculeService.supprimer(id, conducteurId);
    }
}