package com.example.multas.domain.port;

import com.example.multas.domain.ResultadoPago;
import com.example.multas.model.Multa;

// domain/port/PasarelaPagoPort.java
public interface PasarelaPagoPort {
    ResultadoPago procesar(Multa multa);
}