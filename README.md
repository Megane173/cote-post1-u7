# Post-contenido — Unidad 7: Patrones Arquitectónicos I

## Descripción
Repositorio del post-contenido de la Unidad 7 de Patrones de Diseño
de Software. Un unico proyecto Spring Boot (multas-biblioteca-api)
para la gestion de multas de biblioteca, con dos partes: una API REST
en capas (Model, Repository, Service, Controller) sobre H2, y pago en
linea de multas con dos pasarelas intercambiables.

## Parte 1 — Arquitectura en Capas
MultaRepository extiende JpaRepository y agrega una consulta
agregada (countByEstudianteIdAndEstado). MultaService concentra las
reglas de negocio (limite de multas pendientes, generacion); el
calculo del monto vive en la propia entidad Multa
(Multa.calcularMonto). MultaController expone /api/multas. Ver
paquetes model/, repository/, service/ y controller/.

## Parte 2 — Pago en Linea con Dos Pasarelas
[Documentar aqui la opcion elegida entre A (rama condicional), B
(Strategy en service/) o C (puerto de dominio con adaptadores) y por
que. Si se eligio C: domain/port/PasarelaPagoPort y domain/ResultadoPago
son Java puro; infrastructure/pago/PagosUdesAdapter e
infrastructure/pago/WompiAdapter traducen cada formato HTTP externo
al mismo ResultadoPago; se seleccionan por app.pagos.proveedor sin
tocar MultaController ni el resto de MultaService.]

## Cómo ejecutar
```
$ cd multas-biblioteca-api && mvn spring-boot:run
```

## Herramientas utilizadas
- Java 17, Spring Boot 4.1.1, Spring Data JPA, H2, RestTemplate
- Apache Maven, Postman/curl, Git, GitHub, VS code

## Decisiones de diseño

### Punto de decisión 1 — Cálculo del monto: ¿entidad o Service?
El cálculo del monto se ubicó en Multa.calcularMonto porque la regla solo depende de los valores definidos por la propia entidad. De esta forma, Multa no es solamente un contenedor de datos, sino que también contiene un comportamiento propio relacionado directamente con su estado. La alternativa sería colocar el cálculo dentro de MultaService, pero esto haría que la entidad dependiera del Service para una operación que no necesita ningún dato externo. Además, si el cálculo estuviera únicamente en el Service, se acercaría a un modelo anémico, donde las entidades contienen principalmente datos y la lógica de negocio queda concentrada en los servicios.

### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?
Para verificar el límite de multas pendientes se utiliza countByEstudianteIdAndEstado, permitiendo que la base de datos realice directamente el conteo. Se descartó obtener todas las multas mediante findByEstudianteId para después filtrarlas con Java, porque no es necesario recuperar registros completos cuando únicamente se necesita conocer su cantidad. De esta manera, MultaService mantiene la decisión de negocio —si el estudiante puede generar otra multa— mientras que MultaRepository se encarga de obtener de forma directa el dato que esa decisión necesita. Ademas, evita cargar en memoria multas que no son necesarias para realizar la validación.### Punto de decisión 2 — Conteo de multas pendientes: ¿consulta o filtrado en memoria?

### Punto de decisión 3 — Selección del adaptador activo
Se eligió @ConditionalOnProperty para seleccionar el adaptador de pago activo durante el arranque de la aplicación. La propiedad app.pagos.proveedor determina si se crea PagosUdesAdapter o WompiAdapter, mientras que MultaService solo depende de PasarelaPagoPort y no necesita conocer cuál implementación está activa. Esto permite cambiar la pasarela modificando application.properties, sin modificar ni recompilar el Controller o el Service.

La alternativa descartada fue inyectar un Map<String, PasarelaPagoPort> y seleccionar el proveedor en tiempo de ejecución. Aunque esta opción permitiría cambiar de proveedor sin reiniciar la aplicación y facilitaría escenarios donde varias pasarelas estuvieran disponibles simultáneamente, no era necesaria para el requisito planteado: cada sede mantiene una pasarela configurada durante el piloto. Además, esa alternativa haría que MultaService tenga que participar en la desición de que implementación concreta se utiliza, lo que haria que el Service conociera de forma indirecta conceptos de infraestructura y configuración.


### Punto de decisión 4 — Diseño del puerto y el tipo de resultado
Se definió PasarelaPagoPort como un contrato común que recibe una Multa y devuelve ResultadoPago. Este resultado utiliza conceptos neutrales como proveedor, exitoso, referenciaExterna y mensaje, en lugar de exponer los nombres o estructuras particulares de una pasarela. De esta forma, MultaService trabaja con el resultado de un pago sin conocer si fue procesado por PagosUDES o por Wompi.

Esta decisión es especialmente importante porque las dos pasarelas tienen contratos diferentes: PagosUDES utiliza idTransaccion y estadoTransaccion, mientras que Wompi utiliza reference, status y maneja el monto en centavos. Si PasarelaPagoPort devolviera directamente el DTO de PagosUDES, MultaService quedaría acoplado a ese proveedor y Wompi tendría que adaptar su respuesta a una estructura que no le pertenece. De igual manera, utilizar directamente el DTO de Wompi produciría el mismo problema en sentido contrario. Agregar una tercera pasarela obligaría entonces a modificar el Service o a introducir más condiciones. ResultadoPago evita este acoplamiento al actuar como una representación común del resultado que necesitan las capas superiores.

### Trade-off considerado — Parte 2
[Párrafo honesto: qué opción se descartó entre capas extendidas
(A/B) y puerto con adaptadores (C), qué se ganó y qué costó adicional
tuvo la opción elegida —líneas de código, paquetes nuevos, curva de
aprendizaje—, y si el equipo consideraría revertir la decisión si el
piloto de pasarelas terminara y solo quedara una.]

### Documentacion

#### Capturas de pantalla de pruebas con postman en la parte 1

![GET /api/multas retorna lista vacía (200 OK) al inicio.](multas-biblioteca-api/DocumentacionTrabajo/get_multas_200.png)
![POST /api/multas con JSON válido retorna 201 Created con la multa creada y el monto calculado.](multas-biblioteca-api/DocumentacionTrabajo/post_multas.png)
![POST /api/multas sin estudianteId retorna 400 Bad Request con mensaje de validación.](multas-biblioteca-api/DocumentacionTrabajo/post_multas_400.png)
![Generar una cuarta multa pendiente para el mismo estudiante retorna 409 Conflict.](multas-biblioteca-api/DocumentacionTrabajo/post_limite3_409.png)
![GET /api/multas/{id} con ID inexistente retorna 404 Not Found.](multas-biblioteca-api/DocumentacionTrabajo/get_noMultaId_404.png)
![PATCH /api/multas/{id}/pagar marca la multa como PAGADA con metodoPago "VENTANILLA";](multas-biblioteca-api/DocumentacionTrabajo/patch_pagarVentanilla_200.png)
![PATCH /api/multas/{id}/pagar se repite](multas-biblioteca-api/DocumentacionTrabajo/patch_pagarVentanilla_409.png)

## Conclusiones
En la primera parte se aprendió a separar las responsabilidades entre Controller, Service, Model y Repository, dejando las reglas de negocio propias de Multa en la entidad y las de casos de uso en MultaService. En la segunda parte se vio que las dos pasarelas tenían contratos HTTP diferentes, por lo que era conveniente mantener esos detalles separados de la lógica de casos de usos. Lo más difícil fue decidir entre extender las capas existentes o introducir un puerto, porque ambas opciones podían resolver el requisito, pero el puerto requería más clases y estructura. Finalmente, se eligió el puerto de dominio porque permite aislar los detalles de cada pasarela y facilita agregar o cambiar proveedores en el futuro.



