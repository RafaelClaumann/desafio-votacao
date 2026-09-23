package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.service.PautaService;
import com.votacao.entrypoint.api.dto.PautaRequestDTO;
import com.votacao.entrypoint.api.dto.PautaResponseDTO;
import com.votacao.entrypoint.mapper.PautaMapper;
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
    private final PautaMapper mapper;

    public PautaController(PautaService service, PautaMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PautaResponseDTO> save(@RequestBody @Valid final PautaRequestDTO requestBody) {
        Pauta domain = mapper.toDomain(requestBody);
        Pauta saved = service.savePauta(domain);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<PautaResponseDTO>> fetch() {
        List<Pauta> pautas = service.getPautas();
        return ResponseEntity.ok(mapper.toResponseList(pautas));
    }

}
