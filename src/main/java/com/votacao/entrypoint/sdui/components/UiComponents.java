package com.votacao.entrypoint.sdui.components;

import java.util.List;

public class UiComponents {

    public record SduiField(SduiEnums.FieldId id, SduiEnums.FieldType type, String label) {
    }

    public record SduiButton(String label, String url, Object body) {
    }

    public sealed interface SduiItem permits ItemPauta, ItemGenerico {
        String label();

        String url();

        Object body();
    }

    public record ItemGenerico(
            String label,
            String url,
            Object body
    ) implements SduiItem {
    }

    public record ItemPauta(
            String label,
            Long tempoVotacaoMinutos,
            boolean hasSessao,
            String url,
            Object body
    ) implements SduiItem {
    }

    public record TelaFormulario(
            SduiEnums.ScreenType type,
            List<SduiField> fields,
            List<SduiButton> buttons
    ) {
        public TelaFormulario(List<SduiField> fields, List<SduiButton> buttons) {
            this(SduiEnums.ScreenType.FORMULARIO, fields, buttons);
        }
    }

    public record TelaSelecao(
            SduiEnums.ScreenType type,
            List<SduiItem> items
    ) {
        public TelaSelecao(List<SduiItem> items) {
            this(SduiEnums.ScreenType.SELECAO, items);
        }
    }

}
