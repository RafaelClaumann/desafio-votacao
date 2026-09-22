package com.votacao.entrypoint.sdui;

import com.votacao.application.model.Pauta;
import com.votacao.application.service.PautaService;
import com.votacao.application.service.query.PautaComStatus;
import com.votacao.entrypoint.api.dto.PautaDTO;
import com.votacao.entrypoint.sdui.components.UiComponents;
import com.votacao.entrypoint.sdui.screens.PautaScreen;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sdui/pautas")
public class PautaUiController {

    private final PautaService service;

    public PautaUiController(PautaService service) {
        this.service = service;
    }

    @PostMapping("/form")
    public ResponseEntity<UiComponents.TelaFormulario> getPautaForm() {
        UiComponents.TelaFormulario pautaForm = PautaScreen.createPautaFormScreen();
        return ResponseEntity.ok(pautaForm);
    }

    @PostMapping("/action")
    public ResponseEntity<UiComponents.TelaSelecao> createPauta(@RequestBody PautaDTO requestBody) {
        Pauta domain = PautaDTO.toDomain(requestBody);
        service.savePauta(domain);

        List<PautaComStatus> queryResult = service.pautaComStatuses();
        UiComponents.TelaSelecao telaSelecao = PautaScreen.createPautaSelectionScreen(queryResult);
        return ResponseEntity.ok(telaSelecao);
    }

    @PostMapping("/selection")
    public ResponseEntity<UiComponents.TelaSelecao> selectPauta() {
        List<PautaComStatus> queryResult = service.pautaComStatuses();
        UiComponents.TelaSelecao telaSelecao = PautaScreen.createPautaSelectionScreen(queryResult);
        return ResponseEntity.ok(telaSelecao);
    }

}
