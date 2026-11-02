package com.isil.edu.pe.spedidos.service;

import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.service.domain.ClienteFrecuente;
import com.isil.edu.pe.spedidos.service.domain.ProductoVendido;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;

import java.math.BigDecimal;
import java.util.List;

public interface PedidoReporteService {

  ResumenPedidos obtenerResumen();

  List<Pedido> buscarPorCliente(String cliente);

  List<ProductoVendido> listarProductosMasVendidos();

  // Metodo de busqueda de pedidos por cliente y total minimo
  List<Pedido> buscarPedidosPorClienteYTotalMinino(String cliente, BigDecimal totalMinimo);
  // Metodo para listar a clientes frecuentes con un minimo de pedidos
  List<ClienteFrecuente> listarClientesFrecuentes(Long minPedidos);

}
