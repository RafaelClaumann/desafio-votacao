package com.votacao.entrypoint.sdui.screens;

import com.votacao.application.service.query.PautaComStatus;
import com.votacao.entrypoint.sdui.components.UiComponents;

import java.util.List;
import java.util.Map;

import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldId.CPF;
import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldId.PAUTA_ID;
import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldId.TEMPO;
import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldId.TITULO;
import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldType.NUMBER;
import static com.votacao.entrypoint.sdui.components.SduiEnums.FieldType.TEXT;
import static com.votacao.entrypoint.sdui.screens.SessaoScreen.SESSOES_ACTION;

public final class PautaScreen {

    public static final String PAUTAS_FORM = "/sdui/pautas/form";
    public static final String PAUTAS_ACTION = "/sdui/pautas/action";
    public static final String PAUTAS_SELECTION = "/sdui/pautas/selection";

    private PautaScreen() {
    }

    public static UiComponents.TelaFormulario createPautaFormScreen() {
        List<UiComponents.SduiField> fields = List.of(
                new UiComponents.SduiField(TITULO, TEXT, "Título da pauta"),
                new UiComponents.SduiField(TEMPO, NUMBER, "Tempo da sessão"),
                new UiComponents.SduiField(CPF, TEXT, "CPF do responsável")
        );

        List<UiComponents.SduiButton> buttons = List.of(
                new UiComponents.SduiButton("Cadastrar", PAUTAS_ACTION, Map.of())
        );

        return new UiComponents.TelaFormulario(fields, buttons);
    }

    public static UiComponents.TelaSelecao createPautaSelectionScreen(List<PautaComStatus> queryResult) {
        List<UiComponents.SduiItem> items = queryResult.stream()
                .map(query -> (UiComponents.SduiItem) new UiComponents.ItemPauta(
                        query.pauta().titulo(),
                        query.pauta().tempoVotacaoMinutos(),
                        query.hasSessao(),
                        SESSOES_ACTION,
                        Map.of(PAUTA_ID, query.pauta().id())
                ))
                .toList();

        return new UiComponents.TelaSelecao(items);
    }

}
