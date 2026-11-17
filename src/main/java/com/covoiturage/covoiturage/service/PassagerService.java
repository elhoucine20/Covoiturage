package com.covoiturage.covoiturage.service;

import com.covoiturage.covoiturage.dto.passager.PassagerRequest;
import com.covoiturage.covoiturage.dto.passager.PassagerResponse;

import java.util.List;

public interface PassagerService {

    PassagerResponse creer(PassagerRequest request);

    PassagerResponse trouverParId(Long id);

    List<PassagerResponse> lister();

    PassagerResponse modifier(Long id, PassagerRequest request);

    PassagerResponse trouverParEmail(String email);

}
