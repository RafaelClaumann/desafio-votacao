package com.votacao.entrypoint.api;

import com.votacao.application.model.Voto;
import com.votacao.application.service.VotoService;
import com.votacao.entrypoint.api.dto.VotoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/votos")
public class VotoController {

    private final VotoService service;

    public VotoController(VotoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VotoDTO> save(@RequestBody final VotoDTO requestBody) {
        Voto saved = service.votar(
                requestBody.idSessao(),
                requestBody.documento(),
                requestBody.escolhaVoto()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(VotoDTO.fromDomain(saved));
    }

}
