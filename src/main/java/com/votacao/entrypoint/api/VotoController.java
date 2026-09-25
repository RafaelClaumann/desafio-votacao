package com.votacao.entrypoint.api;

import com.votacao.application.model.Voto;
import com.votacao.application.service.VotoService;
import com.votacao.entrypoint.api.dto.VotoDTO;
import com.votacao.entrypoint.api.dto.VotoResponseDTO;
import com.votacao.entrypoint.mapper.VotoMapper;
import jakarta.validation.Valid;
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
    private final VotoMapper mapper;

    public VotoController(VotoService service, VotoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<VotoResponseDTO> save(@RequestBody @Valid final VotoDTO requestBody) {
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

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

}
