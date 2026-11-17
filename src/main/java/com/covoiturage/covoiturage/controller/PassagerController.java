package com.covoiturage.covoiturage.controller;

import com.covoiturage.covoiturage.dto.passager.PassagerRequest;
import com.covoiturage.covoiturage.dto.passager.PassagerResponse;
import com.covoiturage.covoiturage.service.PassagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/passagers")
@RequiredArgsConstructor
public class PassagerController {

    private final PassagerService passagerService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PassagerResponse creer(@Valid @RequestBody PassagerRequest request) {
        return passagerService.creer(request);
    }

    @GetMapping
    public List<PassagerResponse> lister() {
        return passagerService.lister();
    }

    @GetMapping("/search")
    public PassagerResponse rechercherParEmail(@RequestParam("email") String email) {
        return passagerService.trouverParEmail(email);
    }

    @GetMapping("/{id}")
    public PassagerResponse trouverParId(@PathVariable Long id) {
        return passagerService.trouverParId(id);
    }

    @PutMapping("/{id}")
    public PassagerResponse modifier(@PathVariable Long id,
                                     @Valid @RequestBody PassagerRequest request) {
        return passagerService.modifier(id, request);
    }
}