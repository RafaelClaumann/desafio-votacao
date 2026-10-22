package com.votacao.application.gateway;

import java.time.LocalDateTime;

public interface TimeProvider {

    LocalDateTime now();

}
