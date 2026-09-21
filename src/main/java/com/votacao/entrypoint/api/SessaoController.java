package com.votacao.entrypoint.api;

import com.votacao.entrypoint.api.dto.SessaoDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessoes")
public class SessaoController {

    @PostMapping
    public ResponseEntity<String> save(@RequestBody final SessaoDTO requestBody) {
        return ResponseEntity.ok("Sessão criada com sucesso para a pauta: " + requestBody.pautaId());
    }

}
