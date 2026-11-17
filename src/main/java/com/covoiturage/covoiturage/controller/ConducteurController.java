package com.covoiturage.covoiturage.controller;

import com.covoiturage.covoiturage.dto.conducteur.ConducteurRequest;
import com.covoiturage.covoiturage.dto.conducteur.ConducteurResponse;
import com.covoiturage.covoiturage.service.ConducteurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/conducteurs")
@RequiredArgsConstructor
public class ConducteurController {

    private final ConducteurService conducteurService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConducteurResponse creer(@Valid @RequestBody ConducteurRequest request) {
        return conducteurService.creer(request);
    }

    @GetMapping
    public List<ConducteurResponse> lister() {
        return conducteurService.lister();
    }

    @GetMapping("/search")
    public List<ConducteurResponse> rechercher(@RequestParam("q") String critere) {
        return conducteurService.rechercher(critere);
    }

    @GetMapping("/{id}")
    public ConducteurResponse trouverParId(@PathVariable Long id) {
        return conducteurService.trouverParId(id);
    }

    @PutMapping("/{id}")
    public ConducteurResponse modifier(@PathVariable Long id,
                                       @Valid @RequestBody ConducteurRequest request) {
        return conducteurService.modifier(id, request);
    }



}
