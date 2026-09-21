package com.votacao.entrypoint.api;

import com.votacao.application.model.Sessao;
import com.votacao.application.service.SessaoService;
import com.votacao.entrypoint.api.dto.SessaoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/sessoes")
public class SessaoController {

    private final SessaoService service;

    public SessaoController(SessaoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SessaoDTO> save(@RequestBody final SessaoDTO requestBody) {
        Sessao saved = service.saveSessao(requestBody.pautaId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(SessaoDTO.fromDomain(saved));
    }

}
