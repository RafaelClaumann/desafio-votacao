package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.service.PautaService;
import com.votacao.entrypoint.api.dto.PautaRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/pautas")
public class PautaController {

    private final PautaService service;

    public PautaController(PautaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PautaRequestDTO> save(@RequestBody @Valid final PautaRequestDTO requestBody) {
        Pauta domain = PautaRequestDTO.toDomain(requestBody);
        Pauta saved = service.savePauta(domain);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(PautaRequestDTO.fromDomain(saved));
    }

    @GetMapping
    public ResponseEntity<List<PautaRequestDTO>> fetch() {
        List<Pauta> pautas = service.getPautas();
        return ResponseEntity.ok(PautaRequestDTO.toDTOList(pautas));
    }

}
