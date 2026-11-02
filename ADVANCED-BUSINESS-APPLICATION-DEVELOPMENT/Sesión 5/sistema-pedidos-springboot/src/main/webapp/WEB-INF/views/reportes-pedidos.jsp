<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
    <%@ taglib prefix="c" uri="jakarta.tags.core" %>
        <%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
        <%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<!DOCTYPE html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="color-scheme" content="light">
    <title>Reportes · Sistema de Pedidos</title>
    <c:url var="cssUrl" value="/assets/css/reporte.css" />
    <link rel="stylesheet" href="${cssUrl}">
</head>

<body>
    <main class="page">
        <header class="page-header">
            <div>
                <p class="eyebrow">SPRING BOOT 4.1 · SPRING MVC · H2</p>
                <p class="system-name">Sistema de Pedidos <span>/ Reportes</span></p>
                <h1>Un vistazo a tus ventas.</h1>
                <p class="intro">Consulta tus pedidos, descubre los productos más vendidos y conoce a
                    tus mejores clientes.</p>
            </div>

            <c:url var="pedidosUrl" value="/pedidos" />
            <a class="button button-secondary" href="${pedidosUrl}"><span aria-hidden="true">←</span>
                Volver a pedidos</a>

        </header>

        <section class="overview" aria-labelledby="resumen-title">
            <div class="section-heading">
                <h2 id="resumen-title">Resumen general</h2>
                <span class="period"><span class="status-dot" aria-hidden="true"></span> Historial
                    completo</span>
            </div>
            <div class="metrics">
                <article class="metric card">
                    <h3>Total de pedidos</h3>
                    <p class="metric-value">
                        <c:out value="${resumen.totalPedidos}" /><span> pedidos</span>
                    </p>
                    <p class="metric-note">Pedidos registrados</p>
                </article>
                <article class="metric card">
                    <h3>Total de unidades</h3>
                    <p class="metric-value">
                        <c:out value="${resumen.totalUnidades}" /><span> unidades</span>
                    </p>
                    <p class="metric-note">Productos vendidos</p>
                </article>
                <article class="metric card metric-highlight">
                    <h3>Importe acumulado</h3>
                    <p class="metric-value"><span>S/</span>
                        <fmt:formatNumber value="${resumen.importeAcumulado}" minFractionDigits="2"
                                        maxFractionDigits="2" />
                    </p>
                    <p class="metric-note">Total de ventas registradas</p>
                </article>
                <article class="metric card">
                    <h3>Ticket promedio</h3>
                    <p class="metric-value"><span>S/</span>
                        <fmt:formatNumber value="${resumen.ticketPromedio}" minFractionDigits="2"
                                        maxFractionDigits="2" />
                    </p>
                    <p class="metric-note">Importe promedio por pedido</p>
                </article>
            </div>
        </section>

        <section class="card section-card" aria-labelledby="busqueda-title">
            <div class="section-heading">
                <div>
                    <p class="eyebrow section-eyebrow">EXPLORA EL DETALLE</p>
                    <h2 id="busqueda-title">Buscar pedidos</h2>
                    <p class="section-description">Encuentra los pedidos de un cliente y consulta el
                        detalle de sus compras.</p>
                </div>
                <span class="tag">Consulta por cliente</span>
            </div>
            <c:url var="pedidosUrl" value="/reportes/pedidos/buscar"/>
            <form class="search-form" action="${pedidosUrl}" method="get">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="field">
                    <label for="cliente">Nombre del cliente <span class="required">*</span></label>
                    <input id="cliente" name="cliente" type="search" placeholder="Ej. Ana Torres" required
                        maxlength="120" autocomplete="name">
                </div>
                <div class="field">
                    <label for="tMinimo">Total mínimo <span class="optional">(opcional)</span></label>
                    <div class="money-input"><span aria-hidden="true">S/</span><input id="tMinimo"
                            name="tMinimo" type="number" min="0" step="0.01" placeholder="0.00">
                    </div>
                </div>
                <button class="button" type="submit"><svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                        stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                        <circle cx="10.5" cy="10.5" r="6.5" />
                        <path d="m16 16 4.5 4.5" />
                    </svg> Buscar pedidos</button>
            </form>

            <div class="results-heading">
                <h3>Pedidos registrados</h3><span class="muted"></span>
            </div>
            <div class="table-scroll" role="region" aria-label="Detalle de pedidos registrados" tabindex="0">
                <table class="orders-table">
                    <caption class="sr-only">Pedidos con cliente, producto, precio, cantidad, total y
                        fecha</caption>
                    <thead>
                        <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Cliente</th>
                            <th scope="col">Código</th>
                            <th scope="col">Producto</th>
                            <th scope="col" class="numeric">Precio unitario</th>
                            <th scope="col" class="numeric">Cantidad</th>
                            <th scope="col" class="numeric">Total</th>
                            <th scope="col">Fecha</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="pedido" items="${listaPedidos}">
                            <tr>
                                <td class="muted">
                                    <c:out value="${pedido.id}" />
                                </td>
                                <td class="customer">
                                    <c:out value="${pedido.cliente}" />
                                </td>
                                <td><span class="code">
                                        <c:out value="${pedido.productoCodigo}" />
                                    </span></td>
                                <td>
                                    <c:out value="${pedido.productoNombre}" />
                                </td>
                                <td class="numeric">S/
                                    <fmt:formatNumber value="${pedido.precioUnitario}" minFractionDigits="2"
                                        maxFractionDigits="2" />
                                </td>
                                <td class="numeric">
                                    <c:out value="${pedido.cantidad}" />
                                </td>
                                <td class="numeric amount">S/
                                    <fmt:formatNumber value="${pedido.total}" minFractionDigits="2"
                                        maxFractionDigits="2" />
                                </td>
                                <td class="date"><time datetime="2026-11-01T16:40">
                                        <c:out value="${pedido.fecha}" />
                                    </time></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
            <div class="table-footer">
                <span>Importes expresados en soles (PEN)</span>
            </div>
        </section>

        <div class="rankings">
            <section class="card section-card" aria-labelledby="productos-title">
                <div class="section-heading">
                    <div>
                        <p class="eyebrow section-eyebrow">TUS PRODUCTOS DESTACADOS</p>
                        <h2 id="productos-title">Productos más vendidos</h2>
                        <p class="section-description">Ordenados por total de unidades vendidas.</p>
                    </div>
                </div>
                <div class="table-scroll" role="region" aria-label="Ranking de productos" tabindex="0">
                    <table>
                        <caption class="sr-only">Productos más vendidos, de mayor a menor cantidad de
                            unidades</caption>
                        <thead>
                            <tr>
                                <th scope="col">Código / Producto</th>
                                <th scope="col" class="numeric">Pedidos</th>
                                <th scope="col" class="numeric">Unidades</th>
                                <th scope="col" class="numeric">Importe acumulado</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="productoTop" items="${productosMasVendidos}">
                                <tr>
                                    <td><span class="product-name"><c:out value="${productoTop.productoNombre}" /></span><span
                                            class="product-code"><c:out value="${productoTop.productoCodigo}" /></span></td>
                                    <td class="numeric"><c:out value="${productoTop.totalPedidos}" /></td>
                                    <td class="numeric"><span class="units"><c:out value="${productoTop.totalUnidades}" /></span></td>
                                    <td class="numeric amount">S/ <fmt:formatNumber value="${productoTop.importeAcumulado}" minFractionDigits="2"
                                        maxFractionDigits="2" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                            
                    </table>
                </div>
            </section>

            <section class="card section-card" aria-labelledby="clientes-title">
                <div class="section-heading">
                    <div>
                        <p class="eyebrow section-eyebrow">QUIENES MÁS CONFÍAN EN TI</p>
                        <h2 id="clientes-title">Clientes más frecuentes</h2>
                        <p class="section-description">Ordenados por cantidad de pedidos realizados.</p>
                    </div>
                    <span class="count-badge" aria-label="4 clientes">4</span>
                </div>
                
                <c:url var="clientesUrl" value="/reportes/pedidos/clientes/frecuentes"/>
                <form class="clients-filter" action="${clientesUrl}" method="get">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <div class="field">
                        <label for="minPedidos">Mínimo de pedidos</label>
                        <input id="minPedidos" name="minPedidos" type="number" min="1" step="1" value="1" required
                            aria-describedby="minimo-pedidos-ayuda">
                    </div>
                    <button class="button" type="submit">Consultar clientes</button>
                    <p id="minimo-pedidos-ayuda" class="filter-help">Consulta los clientes que tengan esta cantidad de pedidos o
                        más.</p>
                </form>
                <div class="table-scroll" role="region" aria-label="Ranking de clientes frecuentes" tabindex="0">
                    <table>
                        <caption class="sr-only">Clientes más frecuentes con puesto, pedidos, unidades, importe y ticket promedio
                        </caption>
                        <thead>
                            <tr>
                                <th scope="col">Puesto</th>
                                <th scope="col">Cliente</th>
                                <th scope="col" class="numeric">Pedidos</th>
                                <th scope="col" class="numeric">Unidades</th>
                                <th scope="col" class="numeric">Importe acumulado</th>
                                <th scope="col" class="numeric">Ticket promedio</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="clienteTop" items="${clientesFrecuentes}" varStatus="status">
                                <tr>
                                    <td><span class="rank rank-first">${status.count}</span></td>
                                    <td class="customer"><c:out value="${clienteTop.cliente}"/></td>
                                    <td class="numeric"><c:out value="${clienteTop.totalPedidos}"/></td>
                                    <td class="numeric"><c:out value="${clienteTop.totalUnidades}"/></td>
                                    <td class="numeric amount">S/ 
                                        <fmt:formatNumber value="${clienteTop.importeAcumulado}" minFractionDigits="2"
                                        maxFractionDigits="2" />
                                    </td>
                                    <td class="numeric">S/ 
                                        <fmt:formatNumber value="${clienteTop.ticketPromedio}" minFractionDigits="2"
                                        maxFractionDigits="2" /></td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <p class="ranking-note">Ticket promedio = importe acumulado / total de pedidos.</p>
            </section>
        </div>
        <footer class="page-footer"><span>Sistema de Pedidos <span aria-hidden="true">·</span>
                Reportes</span><span>Cada pedido conserva los datos del producto al momento de la
                compra.</span></footer>
    </main>
</body>

</html>