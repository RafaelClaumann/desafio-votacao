package com.votacao.entrypoint.api;

import com.votacao.entrypoint.persistence.PautaEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pautas")
public class PautaController {


    @PostMapping
    public ResponseEntity<PautaDTO> save(@RequestBody final PautaDTO requestBody) {
        final PautaEntity pautaEntity = new PautaEntity(requestBody.titulo(), requestBody.tempoVotacaoSegundos());
        return ResponseEntity.ok(requestBody);
    }

}
