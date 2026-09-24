package com.votacao.entrypoint.client.documentvalidator;

public record ValidatorResponse(Json json) {

    public record Json(String documento) {
    }

}
