package com.covoiturage.covoiturage.dto.passager;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PassagerRequest(

        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 100, message = "Le nom ne doit pas depasser 100 caracteres")
        String nom,

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(max = 100, message = "Le prenom ne doit pas depasser 100 caracteres")
        String prenom,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        @Size(max = 150, message = "L'email ne doit pas depasser 150 caracteres")
        String email,

        @NotBlank(message = "Le telephone est obligatoire")
        @Pattern(regexp = "^\\+?[0-9]{8,15}$",
                message = "Le telephone doit contenir de 8 a 15 chiffres (+ autorise au debut)")
        String telephone,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "8 caracteres minimum")
        String password
) {
}