package com.votacao.entrypoint.api;

import com.votacao.application.model.Sessao;
import com.votacao.application.service.SessaoService;
import com.votacao.application.service.VotoService;
import com.votacao.application.service.query.ApuracaoSessao;
import com.votacao.entrypoint.api.dto.ResultadoVotacaoResponse;
import com.votacao.entrypoint.api.dto.SessaoDTO;
import com.votacao.entrypoint.api.dto.SessaoResponseDTO;
import com.votacao.entrypoint.mapper.SessaoResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/sessoes")
public class SessaoController {

    private final SessaoService service;
    private final VotoService votoService;
    private final SessaoResponseMapper mapper;

    public SessaoController(SessaoService service, VotoService votoService, SessaoResponseMapper mapper) {
        this.service = service;
        this.votoService = votoService;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<SessaoResponseDTO> save(@RequestBody final SessaoDTO requestBody) {
        Sessao saved = service.saveSessao(requestBody.pautaId());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toDTO(saved));
    }

    @GetMapping
    public ResponseEntity<List<SessaoResponseDTO>> fetch() {
        List<Sessao> sessoes = service.getSessoes();
        return ResponseEntity.ok(mapper.toDTOList(sessoes));
    }

    @GetMapping("/{idSessao}/resultado")
    public ResponseEntity<ResultadoVotacaoResponse> apurar(@PathVariable long idSessao) {
        ApuracaoSessao resultado = votoService.apurarVotosSessao(idSessao);
        return ResponseEntity.ok(ResultadoVotacaoResponse.fromDomain(resultado));
    }

}
