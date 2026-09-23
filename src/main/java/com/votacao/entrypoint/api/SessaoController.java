package com.votacao.entrypoint.api;

import com.votacao.application.model.Sessao;
import com.votacao.application.service.SessaoService;
import com.votacao.application.service.VotoService;
import com.votacao.application.service.query.ApuracaoSessao;
import com.votacao.entrypoint.api.dto.ResultadoVotacaoResponse;
import com.votacao.entrypoint.api.dto.SessaoDTO;
import com.votacao.entrypoint.api.dto.SessaoResponseDTO;
import com.votacao.entrypoint.mapper.SessaoMapper;
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
    private final SessaoMapper mapper;

    /**
     * Cria o controlador de sessões.
     *
     * @param service serviço de sessões
     * @param votoService serviço de votação para apuração
     * @param mapper conversor entre sessões e DTOs
     */
    public SessaoController(SessaoService service, VotoService votoService, SessaoMapper mapper) {
        this.service = service;
        this.votoService = votoService;
        this.mapper = mapper;
    }

    /**
     * Abre uma nova sessão para uma pauta.
     *
     * @param requestBody dados da sessão
     * @return sessão criada com URI de localização
     */
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

    /**
     * Lista todas as sessões cadastradas.
     *
     * @return lista de sessões
     */
    @GetMapping
    public ResponseEntity<List<SessaoResponseDTO>> fetch() {
        List<Sessao> sessoes = service.getSessoes();
        return ResponseEntity.ok(mapper.toDTOList(sessoes));
    }

    /**
     * Apura o resultado de uma sessão fechada.
     *
     * @param idSessao identificador da sessão
     * @return resultado da apuração
     */
    @GetMapping("/{idSessao}/resultado")
    public ResponseEntity<ResultadoVotacaoResponse> apurar(@PathVariable long idSessao) {
        ApuracaoSessao resultado = votoService.apurarVotosSessao(idSessao);
        return ResponseEntity.ok(mapper.toResponse(resultado));
    }

}
