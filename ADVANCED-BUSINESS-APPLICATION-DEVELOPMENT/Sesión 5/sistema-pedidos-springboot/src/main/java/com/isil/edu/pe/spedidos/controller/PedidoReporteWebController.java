package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.controller.dto.ClienteFrecuenteDto;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.exception.PedidoException;
import com.isil.edu.pe.spedidos.service.PedidoReporteService;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reportes/pedidos")
public class PedidoReporteWebController {

    private static final Logger log = LoggerFactory.getLogger(PedidoReporteWebController.class);

    private final PedidoReporteService reporteService;

    public PedidoReporteWebController(PedidoReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String mostrarReportes(Model model, HttpServletResponse response) {
        try {
            cargarReportes(model);
            return "reportes-pedidos";
        } catch (InventarioNoDisponibleException e) {
            return mostrarServicioNoDisponible(model, response, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado al cargar la pantalla de reportes.", e);
            return mostrarErrorGeneral(model, response);
        }
    }

    @GetMapping (value = "/buscar")
    public String buscarPedidosPorClienteYOTotalMinimo(
            @RequestParam String cliente,
            @RequestParam(required = false) String tMinimo,
            Model model, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        try {
            cargarReportes(model);
            List<Pedido> pedidos;

            if (tMinimo == null || tMinimo.trim().isEmpty()) {
                pedidos = reporteService.buscarPorCliente(cliente);
            } else {
                BigDecimal totalMinimo = new BigDecimal(tMinimo);
                pedidos = reporteService.buscarPedidosPorClienteYTotalMinino(cliente, totalMinimo);
            }
            model.addAttribute("listaPedidos", pedidos);
            return "reportes-pedidos";

        } catch (NumberFormatException e) {
            return mostrarErrorNegocio(model, response, cliente, tMinimo,null,
                    "El total minimo ingresado es inválida.");
        } catch (InventarioNoDisponibleException e) {
            return mostrarServicioNoDisponible(model, response, e.getMessage());
        } catch (PedidoException e) {
            return mostrarErrorNegocio(model, response, cliente, tMinimo, null,
                    e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado al buscar los pedidos.", e);
            return mostrarErrorGeneral(model, response);
        }
    }
    @GetMapping (value = "/clientes/frecuentes")
    public String buscarClientesFrecuentes(
        @RequestParam String minPedidos,
        Model model, HttpServletResponse response, RedirectAttributes redirectAttributes
    ) {
        try {
            cargarReportes(model);
            List<ClienteFrecuenteDto> clientesFrecuentes = reporteService.listarClientesFrecuentes(Long.parseLong(minPedidos))
                .stream()
                .map(ClienteFrecuenteDto::from)
                .toList();
            model.addAttribute("clientesFrecuentes", clientesFrecuentes);
            return "reportes-pedidos";

        } catch (NumberFormatException e) {
            return mostrarErrorNegocio(model, response, null, null, minPedidos,
                    "El mínimo de pedidos ingresado es inválido.");
        } catch (InventarioNoDisponibleException e) {
            return mostrarServicioNoDisponible(model, response, e.getMessage());
        } catch (PedidoException e) {
            return mostrarErrorNegocio(model, response, null, null, minPedidos,
                    e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error inesperado al buscar los pedidos.", e);
            return mostrarErrorGeneral(model, response);
        }
    }
    // Carga de reportes iniciales
    private void cargarReportes(Model model) {
        model.addAttribute("resumen", reporteService.obtenerResumen());
        model.addAttribute("productosMasVendidos", reporteService.listarProductosMasVendidos());
        model.addAttribute("clientesFrecuentes", reporteService.listarClientesFrecuentes(1L)
                .stream()
                .map(ClienteFrecuenteDto::from)
                .toList());
    }

    // Manejo de errores
    private String mostrarErrorNegocio(Model model, HttpServletResponse response, String cliente,
            String tMinimo, String pedidosMinimo, String mensaje) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());

        model.addAttribute("clienteIngresado", cliente);
        model.addAttribute("totalMinimo", tMinimo);
        model.addAttribute("pedidosMinimo", pedidosMinimo);
        model.addAttribute("error", mensaje);

        try {
            cargarReportes(model);
            return "reportes-pedidos";
        } catch (InventarioNoDisponibleException e) {
            return mostrarServicioNoDisponible(model, response, e.getMessage());
        }
    }

    private String mostrarServicioNoDisponible(
            Model model,
            HttpServletResponse response,
            String mensaje) {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        model.addAttribute("error", mensaje);
        return "error";
    }

    private String mostrarErrorGeneral(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("error", "Ocurrió un error interno al procesar la solicitud.");
        return "error";
    }
}
