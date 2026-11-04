package com.isil.edu.pe.spedidos.controller;

import com.isil.edu.pe.spedidos.client.InventarioClient;
import com.isil.edu.pe.spedidos.client.InventarioNoDisponibleException;
import com.isil.edu.pe.spedidos.controller.dto.ClienteFrecuenteDto;
import com.isil.edu.pe.spedidos.entity.Pedido;
import com.isil.edu.pe.spedidos.repository.PedidoReporteJdbcRepository;
import com.isil.edu.pe.spedidos.service.domain.ResumenPedidos;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoTestGeneral {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private InventarioClient inventarioClient;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        jdbcTemplate.update("DELETE FROM pedido");

        insertarPedido("Ana Torres", "MON-001", "Monitor", "850.00", 1, "850.00");
        insertarPedido("Ana Torres", "TEC-001", "Teclado", "120.00", 2, "240.00");
        insertarPedido("Carlos Perez", "LAP-001", "Laptop", "2500.00", 1, "2500.00");
  
        clearInvocations(inventarioClient);
    }

    // CASO 1: búsqueda por cliente y monto mínimo
    @Test
    @DisplayName("JSP: recibe únicamente los pedidos que cumplen ambos filtros")
    @SuppressWarnings("unchecked")
    void buscarPedidosWeb() throws Exception {
        // Arrange: Ana tiene pedidos de 850 y 240; Carlos, uno de 2500.
        String cliente = "Ana";
        String totalMinimo = "850.00";

        // Act
        ResultActions respuesta = mockMvc.perform(get("/reportes/pedidos/buscar")
                .with(user("consulta").roles("CONSULTA"))
                .param("cliente", cliente)
                .param("tMinimo", totalMinimo));

        // Assert
        MvcResult resultado = respuesta.andExpect(status().isOk())
                .andExpect(view().name("reportes-pedidos"))
                .andExpect(model().attributeExists("listaPedidos"))
                .andReturn();

        List<Pedido> pedidos = (List<Pedido>) resultado.getModelAndView()
                .getModel().get("listaPedidos");
                
        assertThat(pedidos).singleElement().satisfies(pedido -> {
            assertThat(pedido.getCliente()).isEqualTo("Ana Torres");
            assertThat(pedido.getProductoCodigo()).isEqualTo("MON-001");
            assertThat(pedido.getTotal()).isEqualByComparingTo(totalMinimo);
        });
        verifyNoInteractions(inventarioClient);
    }

    // CASO 2: clientes frecuentes y consolidado
    @Test
    @DisplayName("Web: minPedidos=2 excluye a Carlos y calcula todos los indicadores")
    @SuppressWarnings("unchecked")
    void clientesFrecuentesWeb() throws Exception {
        // Arrange: datos de setUp; solo Ana alcanza dos pedidos.

        // Act
        MvcResult resultado = mockMvc.perform(get("/reportes/pedidos/clientes/frecuentes")
                .with(user("consulta").roles("CONSULTA"))
                .param("minPedidos", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("reportes-pedidos"))
                .andExpect(model().attributeExists("clientesFrecuentes"))
                .andReturn();
        // Assert
        
        List<ClienteFrecuenteDto> clientesFrecuentes = 
            (List<ClienteFrecuenteDto>) resultado
                .getModelAndView()
                .getModel()
                .get("clientesFrecuentes");

        assertThat(clientesFrecuentes).hasSize(1);
        ClienteFrecuenteDto cliente = clientesFrecuentes.get(0);

        assertThat(cliente.cliente()).isEqualTo("Ana Torres");
        assertThat(cliente.totalPedidos()).isEqualTo(2);
        verifyNoInteractions(inventarioClient);

    }

    // CASO 3: parámetros inválidos
    @Test
    @MethodSource("parametrosInvalidos")
    @DisplayName("Controller: rechaza parámetros inválidos con HTTP 400")
    void rechazarParametrosInvalidosEnReportes()
            throws Exception {
        // Arrange
        var solicitud = get("/reportes/pedidos/clientes/frecuentes")
                .with(user("consulta").roles("CONSULTA"))
                .param("minPedidos", "-1");

        // Act
        ResultActions respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido",
                Long.class)).isEqualTo(3L);
        verifyNoInteractions(inventarioClient);
    }

    // CASO 4: inventario no disponible
    @Test
    @DisplayName("Inventario caído: registro devuelve 503 y reportes siguen disponibles")
    void inventarioCaidoNoImpideConsultarReportes() throws Exception {
        when(inventarioClient.reservarStock("PRO-001", 1))
                .thenThrow(new InventarioNoDisponibleException(
                        "El servicio de inventario no está disponible."));

        mockMvc.perform(post("/pedidos")
                .with(user("operador").roles("OPERADOR"))
                .with(csrf())
                .param("cliente", "Cliente nuevo")
                .param("productoCodigo", "PRO-001")
                .param("cantidad", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("error"))
                .andExpect(model().attribute(
                        "error",
                        "El servicio de inventario no está disponible."));

        verify(inventarioClient).reservarStock("PRO-001", 1);
        clearInvocations(inventarioClient);

        // El intento fallido no debe dejar un pedido guardado.
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido",
                Long.class)).isEqualTo(3L);

        mockMvc.perform(get("/reportes/pedidos")
                .with(user("consulta").roles("CONSULTA")))
                .andExpect(status().isOk())
                .andExpect(view().name("reportes-pedidos"))
                .andExpect(model().attributeExists(
                        "resumen",
                        "productosMasVendidos",
                        "clientesFrecuentes"));

        verifyNoInteractions(inventarioClient);
    }

    // CASO 5: roles y CSRF
    @Test
    @DisplayName("Anónimo: reportes web y JSON requieren autenticación")
    void reportesRequierenAutenticacion() throws Exception {
        mockMvc.perform(get("/reportes/pedidos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));

        mockMvc.perform(get("/api/v1/pedidos/estadisticas/resumen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login")));
    }

    @Test
    @DisplayName("CONSULTA con CSRF válido no puede registrar pedidos")
    void consultaNoPuedeRegistrarAunqueTengaCsrf() throws Exception {
        mockMvc.perform(post("/pedidos")
                .with(user("consulta").roles("CONSULTA"))
                .with(csrf())
                .param("cliente", "Cliente nuevo")
                .param("productoCodigo", "PRO-001")
                .param("cantidad", "1"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido",
                Long.class)).isEqualTo(3L);

        verifyNoInteractions(inventarioClient);
    }

    @Test
    @DisplayName("OPERADOR sin CSRF no puede registrar pedidos")
    void operadorSinCsrfEsRechazado() throws Exception {
        mockMvc.perform(post("/pedidos")
                .with(user("operador").roles("OPERADOR"))
                .param("cliente", "Cliente nuevo")
                .param("productoCodigo", "PRO-001")
                .param("cantidad", "1"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pedido",
                Long.class)).isEqualTo(3L);
        verifyNoInteractions(inventarioClient);
    }

    private void insertarPedido(
            String cliente,
            String productoCodigo,
            String productoNombre,
            String precioUnitario,
            int cantidad,
            String total) {

        jdbcTemplate.update(
                """
                        INSERT INTO pedido (
                          cliente,
                          producto_codigo,
                          producto_nombre,
                          precio_unitario,
                          cantidad,
                          total,
                          fecha
                        ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                        """,
                cliente,
                productoCodigo,
                productoNombre,
                new BigDecimal(precioUnitario),
                cantidad,
                new BigDecimal(total));
    }
}
