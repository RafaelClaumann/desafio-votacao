package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.service.PautaService;
import com.votacao.entrypoint.api.dto.PautaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaDTO> save(@RequestBody @Valid final PautaDTO requestBody) {
        Pauta domain = PautaDTO.toDomain(requestBody);
        Pauta saved = service.savePauta(domain);
        return ResponseEntity.ok(PautaDTO.fromDomain(saved));
    }

}
