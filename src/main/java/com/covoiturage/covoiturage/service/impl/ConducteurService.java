package com.covoiturage.covoiturage.service.impl;

import com.covoiturage.covoiturage.dto.conducteur.ConducteurRequest;
import com.covoiturage.covoiturage.dto.conducteur.ConducteurResponse;

import java.util.List;

public interface ConducteurService {

    ConducteurResponse creer(ConducteurRequest request);

    ConducteurResponse trouverParId(Long id);

    List<ConducteurResponse> lister();

    ConducteurResponse modifier(Long id, ConducteurRequest request);

    List<ConducteurResponse> rechercher(String critere);

}
