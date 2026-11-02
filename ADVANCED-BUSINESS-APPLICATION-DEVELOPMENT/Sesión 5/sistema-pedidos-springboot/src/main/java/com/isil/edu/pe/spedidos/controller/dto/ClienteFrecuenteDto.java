package com.isil.edu.pe.spedidos.controller.dto;

import java.math.BigDecimal;

import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;


public record ClienteFrecuenteDto(
    String cliente, 
    long totalPedidos, 
    long totalUnidades, 
    BigDecimal importeAcumulado,
    BigDecimal ticketPromedio) 
{
  public static ClienteFrecuenteDto from(ClienteFrecuente cliente) {
    return new ClienteFrecuenteDto(
        cliente.cliente(),
        cliente.totalPedidos(),
        cliente.totalUnidades(),
        cliente.importeAcumulado(),
        cliente.ticketPromedio()
    );
  }
}