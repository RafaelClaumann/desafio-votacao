package com.votacao.entrypoint.sdui.components;

import com.fasterxml.jackson.annotation.JsonValue;

public class SduiEnums {

    public enum ScreenType {
        FORMULARIO, SELECAO;
    }

    public enum FieldType {
        TEXT, NUMBER, DATE;
    }

    public enum FieldId {
        TITULO("titulo"),
        TEMPO("tempo"),
        CPF("cpf"),
        VOTO("voto"),
        PAUTA_ID("pautaId");

        private final String id;

        FieldId(String id) {
            this.id = id;
        }

        @JsonValue
        public String getId() {
            return id;
        }
    }
}