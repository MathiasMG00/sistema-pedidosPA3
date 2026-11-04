# Avance Pa3

## Resumen

El avance de la PA3 amplía el sistema de pedidos con consultas avanzadas, análisis de clientes frecuentes y una interfaz web de reportes. Estas funcionalidades se integraron en las capas de repositorio, servicio y controlador, junto con validaciones de negocio, reglas de acceso y nuevas pruebas de integración.

## 1. Punto de partida: proyecto inicial

El proyecto inicial es sobre el repositorio compartido `https://github.com/carnol1989/isil-2026-ii.git` de la rama `feature/sistema-pedidos-spring-jdbc`:

- Arquitectura por capas con controladores, servicios, repositorios y entidad `Pedido`.
- Registro y consulta de pedidos mediante una interfaz web con JSP.
- Integración HTTP con el servicio de inventario para consultar productos y reservar stock.
- Persistencia de pedidos con JPA y consultas de reportes mediante JDBC.
- Reportes REST de búsqueda por cliente, resumen general y productos más vendidos, incluyendo navegación HAL en el reporte de productos.
- Seguridad con Spring Security, roles `OPERADOR` y `CONSULTA`, autenticación y protección CSRF.
- Pruebas de servicios, repositorios, controladores y seguridad.

## 2. Dependencia de SQL Server en el servicio de inventario

Se incorporó el controlador JDBC de Microsoft SQL Server (`mssql-jdbc`) al archivo `pom.xml` de `Sesión 4/inventario-service`.

Este cambio aporta la dependencia necesaria para conectarse a SQL Server desde el servicio de inventario. 

## 3. Búsqueda avanzada de pedidos

Se implementó una consulta que combina el nombre del cliente con un importe mínimo del pedido.

### Comportamiento

- Busca coincidencias parciales del nombre del cliente sin distinguir mayúsculas y minúsculas.
- Incluye únicamente pedidos cuyo total sea mayor o igual al importe indicado.
- En la interfaz web permite buscar solo por cliente cuando no se ingresa un total mínimo.

### Implementación

- Se añadió la consulta SQL a `PedidoReporteJdbcRepository`, con parámetros nombrados mediante `NamedParameterJdbcTemplate`.
- Se amplió el contrato `PedidoReporteService` y su implementación para exponer la consulta y validar los datos.
- Se incorporó la operación REST `GET /api/v1/pedidos/busqueda-avanzada`, con los parámetros `cliente` y `totalMinimo`.
- Se incorporó la búsqueda web `GET /reportes/pedidos/buscar`, con los parámetros `cliente` y `tMinimo`.

**Resultado:** se pueden localizar pedidos de un cliente que cumplan un umbral de compra, reutilizando la misma lógica de negocio desde la API y la vista web.

## 4. Reporte de clientes frecuentes

Se agregó un reporte que agrupa los pedidos por cliente y permite filtrar a quienes alcanzan una cantidad mínima de pedidos.

### Información del reporte

| Indicador | Descripción |
| --- | --- |
| Cliente | Nombre del cliente registrado en los pedidos. |
| Total de pedidos | Cantidad de pedidos realizados. |
| Total de unidades | Suma de las cantidades compradas. |
| Importe acumulado | Suma del total de sus pedidos. |
| Ticket promedio | Promedio del importe de sus pedidos. |

La consulta utiliza `GROUP BY`, funciones de agregación y `HAVING` para aplicar el mínimo de pedidos. Los clientes se ordenan por cantidad de pedidos descendente.

### Integración por capas

- **Repositorio:** consulta agregada y proyección `repository/projection/ClienteFrecuenteDto`.
- **Servicio:** nuevo modelo de dominio `ClienteFrecuente` y método para consultar clientes frecuentes.
- **Controlador:** DTO de salida `controller/dto/ClienteFrecuenteDto`, con conversión desde el modelo de dominio.
- **API REST:** `GET /api/v1/pedidos/clientes-frecuentes`, con el parámetro `minPedidos`.
- **Interfaz web:** `GET /reportes/pedidos/clientes/frecuentes`, con el parámetro `minPedidos`.

**Resultado:** el sistema permite identificar clientes recurrentes y consultar sus indicadores de compra.

## 5. Nueva pantalla web de reportes

Se creó `PedidoReporteWebController` y la vista `reportes-pedidos.jsp`, accesible desde `/reportes/pedidos`.

La pantalla reúne:

1. **Resumen general:** total de pedidos, total de unidades, importe acumulado y ticket promedio.
2. **Búsqueda de pedidos:** formulario por cliente con total mínimo opcional y tabla de resultados.
3. **Productos más vendidos:** listado con cantidad de pedidos, unidades e importe acumulado.
4. **Clientes frecuentes:** filtro por mínimo de pedidos y tabla con indicadores por cliente.

También se incorporaron navegación desde la pantalla de pedidos, un enlace de retorno y una hoja de estilos propia (`reporte.css`) con adaptación a distintos tamaños de pantalla, tablas con desplazamiento horizontal y formato monetario en soles.

El resumen general y el ranking de productos reutilizan servicios que ya existían en el proyecto inicial; el avance los integra en una pantalla web junto con las nuevas consultas. (Diseño no original)

## 6. Validaciones y manejo de errores

Se reforzaron las validaciones de las consultas de reportes:

- El cliente no puede ser nulo, vacío ni contener únicamente espacios.
- El total mínimo no puede ser nulo ni negativo cuando se utiliza la búsqueda combinada.
- El mínimo de pedidos debe ser mayor o igual a uno.

Las entradas inválidas pasan a generar `PedidoException`, en lugar de devolver listas vacías. Esto permite diferenciar un error en los parámetros de una consulta válida sin resultados.

El nuevo controlador web contempla respuestas **HTTP 400** para errores de entrada o negocio, **HTTP 503** para `InventarioNoDisponibleException` y **HTTP 500** para errores inesperados. También incorpora el registro de errores inesperados mediante un logger.

## 7. Acceso protegido a los reportes

Se amplió `SecurityConfig` para permitir las solicitudes `GET /reportes/**` a usuarios con los roles `OPERADOR` o `CONSULTA`.

Así, las nuevas pantallas de reportes quedan integradas en el esquema de autenticación y autorización existente. Las nuevas pruebas también comprueban restricciones del registro de pedidos y la protección CSRF.

## 8. Siete nuevas pruebas de integración

Se añadieron las 5 pruebas solicitadas más 2 adicionales para la autenticación con Spring Boot, MockMvc, Spring Security y acceso JDBC a la base de datos del perfil `test`. El cliente de inventario se reemplaza por un mock y las pruebas se ejecutan dentro de transacciones.

| Caso | Verificación implementada |
| --- | --- |
| Búsqueda por cliente y monto mínimo | La respuesta web contiene únicamente el pedido que cumple ambos filtros, incluyendo el límite exacto del importe. |
| Clientes frecuentes | Con un mínimo de dos pedidos, se incluye a Ana Torres y se excluye al cliente con un solo pedido; se comprueban el nombre y la cantidad de pedidos. |
| Parámetros inválidos | Un mínimo de pedidos negativo devuelve HTTP 400 y no modifica los pedidos almacenados. |
| Inventario no disponible | El registro fallido devuelve HTTP 503 y no guarda un pedido; posteriormente los reportes siguen disponibles sin invocar inventario. |
| Autenticación | El acceso anónimo al reporte web y al resumen JSON redirige al inicio de sesión. |
| Restricción por rol | Un usuario `CONSULTA` no puede registrar pedidos aunque envíe un token CSRF válido. |
| Protección CSRF | Un usuario `OPERADOR` no puede registrar pedidos si omite el token CSRF. |

Estas pruebas amplían la cobertura sobre los filtros, el acceso protegido y la independencia de las consultas de reportes respecto del servicio de inventario. La descripción corresponde a las verificaciones presentes en el código de pruebas.

## 9. Síntesis del avance

El proyecto evolucionó desde una base con registro de pedidos y reportes REST hacia un módulo de reportes accesible desde la web, con búsqueda avanzada y análisis de clientes frecuentes.

Los aportes más relevantes son:

- Dos nuevas consultas de negocio integradas de extremo a extremo.
- Una pantalla que centraliza indicadores, búsquedas y rankings.
- Validaciones explícitas para distinguir parámetros inválidos de consultas sin resultados.
- Reglas de acceso para las nuevas rutas web.
- Siete pruebas de integración adicionales sobre funcionalidad, disponibilidad y seguridad.