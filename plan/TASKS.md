# Plan — Similar Products API (Backend Dev Test)

**Objetivo:** Spring Boot app en el puerto **5000** que expone `GET /product/{productId}/similar` (contrato `similarProducts.yaml`), orquestando las APIs existentes en `http://localhost:3001`:
- `GET /product/{productId}/similarids` → lista de IDs similares
- `GET /product/{productId}` → detalle del producto

**Criterios de evaluación:** claridad/mantenibilidad, rendimiento, resiliencia, arquitectura hexagonal obligatoria, código mínimo, tests unitarios + ≥5 casuísticas funcionales/e2e, API-first, gestión de excepciones, README, SwaggerUI y Docker.

**Seguimiento:** ver [STATUS.md](./STATUS.md).

---

## Fase 1 — Setup del proyecto

### T01 — Inicializar repo Git público
**Descripción:** Crear el repositorio local y remoto donde vivirá la solución, listo para entregarse como repo "abierto".
**Pasos:**
1. `git init` en la raíz del proyecto.
2. Añadir `.gitignore` para Java/Gradle/IDE (`build/`, `.gradle/`, `.idea/`, `*.iml`, `.DS_Store`).
3. Primer commit con el plan (`plan/`).
4. Crear repo público en GitHub y configurar `origin`.
**Criterios de aceptación:**
- `git status` limpio tras el commit inicial.
- Repo público accesible por URL; push funcionando.
**Entregable:** repositorio público con historial de commits limpio y mensajes descriptivos (idealmente convencionales: `feat:`, `test:`, `docs:`…).

### T02 — Proyecto Spring Boot + dependencias mínimas (multi-módulo)
**Descripción:** Esqueleto Gradle multi-módulo de Spring Boot (Java 21, Spring Boot 4.1.x) con solo las dependencias que aportan valor — nada de código ni librerías muertas. Cada capa de la arquitectura hexagonal vive en su propio módulo Gradle (ver detalle de módulos en T04).
**Pasos:**
1. Generar el proyecto raíz (Spring Initializr con Gradle/Kotlin DSL o Groovy DSL, o a mano): groupId coherente (p. ej. `com.gft.similarproducts`).
2. Crear `settings.gradle`(`.kts`) con `rootProject.name` y los submódulos: `include("domain", "application", "infrastructure")`.
3. `build.gradle`(`.kts`) raíz con configuración común a todos los módulos (`subprojects { ... }`): versión de Java 21, repositorios, gestión de versiones vía BOM de Spring Boot (`io.spring.dependency-management` o `platform()`).
4. Dependencias por módulo (detalle en T04): `infrastructure` depende de `application`, que depende de `domain`; solo `infrastructure` trae `spring-boot-starter-webflux`, `springdoc-openapi-starter-webflux-ui`, etc. `domain` sin dependencias de Spring. `lombok`, `spring-boot-starter-test`, `reactor-test` donde corresponda.
5. Configurar `server.port: 5000` en `application.yml` (módulo `infrastructure`, único que aplica el plugin `org.springframework.boot` y contiene la clase `@SpringBootApplication`).
6. Arrancar la app vacía (`./gradlew :infrastructure:bootRun`) y verificar que escucha en el 5000.
**Criterios de aceptación:**
- `./gradlew clean build` pasa para todos los módulos.
- La app arranca en `:5000` desde el módulo `infrastructure`.
- Ningún `build.gradle`(`.kts`) contiene dependencias sin uso justificado; `domain` no depende de Spring.
**Entregable:** proyecto multi-módulo (`domain`, `application`, `infrastructure`) compilable y arrancable, `application.yml` con puerto y placeholders de configuración.

### T03 — API-first: contrato + openapi-generator
**Descripción:** Partir del contrato `similarProducts.yaml` como fuente de verdad: generar interfaz del controller y DTOs en build, en vez de escribirlos a mano. El generador solo aplica al módulo `infrastructure` (es el único que expone el controller REST).
**Pasos:**
1. Copiar `similarProducts.yaml` (y revisar `existingApis.yaml` para el cliente) desde `/Users/juliansanchez/Documents/development/tecnical-interview/backendDevTest` a `infrastructure/src/main/resources/api/`. ⚠️ Verificar aquí el esquema exacto de `ProductDetail` y de los errores (pendiente de leer los YAML).
2. Configurar el plugin `org.openapi.generator` de Gradle (generator `spring`, opción `interfaceOnly=true`, `reactive=true` si se usa WebFlux, `useSpringBoot3=true` — verificar en su momento si el generator ya expone una opción específica para Spring Boot 4/Spring Framework 7), declarado en `infrastructure/build.gradle`(`.kts`) y ejecutado en una tarea enlazada a `compileJava` de ese módulo.
3. Excluir el código generado de `src` versionado (queda en `infrastructure/build/generated`).
4. Compilar y comprobar que se generan `ProductApi` (o similar) y el DTO `ProductDetail` dentro del módulo `infrastructure`.
**Criterios de aceptación:**
- El build genera las interfaces/DTOs automáticamente; ningún DTO del contrato escrito a mano.
- Un cambio en el YAML se refleja al recompilar.
- El código/DTO generado no se filtra a `domain` ni `application` (el modelo propio de dominio, ver T04, es el único que cruza esa frontera).
**Entregable:** plugin configurado en `infrastructure/build.gradle`(`.kts`), contrato versionado en `infrastructure/src/main/resources/api/`.

## Fase 2 — Arquitectura hexagonal

### T04 — Estructura hexagonal (módulos Gradle y puertos)
**Descripción:** Definir el esqueleto de la arquitectura como **tres módulos Gradle independientes** (`domain`, `application`, `infrastructure`), no solo paquetes dentro de un único módulo. Cada módulo tiene su propio `build.gradle`(`.kts`) y sus dependencias declaran explícitamente la regla de capas — un error de capa se detecta en compilación, no solo por convención.
**Estructura objetivo:**
```
backend-test/
├── settings.gradle(.kts)                 (include: domain, application, infrastructure)
├── build.gradle(.kts)                    (config común vía subprojects/allprojects)
├── domain/
│   ├── build.gradle(.kts)                (sin dependencias de Spring; solo Java estándar + test)
│   └── src/main/java/com/gft/similarproducts/domain
│       ├── model/ProductDetail.java      (record/POJO puro, sin anotaciones de framework)
│       ├── port/in/GetSimilarProductsUseCase.java
│       ├── port/out/ProductClientPort.java   (getSimilarIds, getProductDetail)
│       └── exception/ProductNotFoundException.java
├── application/
│   ├── build.gradle(.kts)                (depende de `project(":domain")`; sin Spring Web/Boot, como mucho `spring-context` si se anota `@Service`)
│   └── src/main/java/com/gft/similarproducts/application
│       └── SimilarProductsService.java   (implementa el use case)
└── infrastructure/
    ├── build.gradle(.kts)                (depende de `project(":application")`; único módulo con `spring-boot-starter-webflux`, plugin `org.springframework.boot`, clase `@SpringBootApplication`)
    └── src/main/java/com/gft/similarproducts/infrastructure
        ├── adapter/in/rest/    (controller + exception handler + mapper DTO↔dominio)
        ├── adapter/out/http/   (cliente WebClient + config)
        └── config/             (beans, propiedades)
```
**Pasos:**
1. Crear los tres módulos en `settings.gradle`(`.kts`) y su `build.gradle`(`.kts`) respectivo, con dependencias entre proyectos: `application` → `implementation(project(":domain"))`; `infrastructure` → `implementation(project(":application"))` (que arrastra transitivamente `domain`).
2. Dentro de cada módulo, paquetes e interfaces de puertos con sus firmas (reactivas: `Mono`/`Flux`, o síncronas si se opta por MVC).
3. Modelo de dominio `ProductDetail` propio en el módulo `domain` (no reutilizar el DTO generado en `infrastructure`).
4. Documentar en el README la regla de dependencias entre módulos: `infrastructure → application → domain`, nunca al revés (reforzada por el propio grafo de dependencias de Gradle, no solo por convención de paquetes).
**Criterios de aceptación:**
- El módulo `domain` no declara ninguna dependencia de Spring ni del código generado por openapi-generator; su `build.gradle`(`.kts`) lo demuestra.
- `application` no depende de `spring-boot-starter-webflux`/`webmvc` ni de librerías HTTP.
- Los adaptadores (`infrastructure`) solo se comunican con el núcleo a través de los puertos expuestos por `domain`/`application`.
- `./gradlew :domain:build :application:build :infrastructure:build` compila cada módulo de forma independiente.
**Entregable:** proyecto Gradle multi-módulo compilable, con `domain`, `application` e `infrastructure` como módulos separados, interfaces de puertos y modelo de dominio.

### T05 — Dominio y caso de uso SimilarProducts
**Descripción:** Implementar `SimilarProductsService`: dado un `productId`, obtener los ids similares y resolver el detalle de cada uno.
**Reglas de negocio:**
- Si el producto base no existe → `ProductNotFoundException` (→ 404).
- Ids similares se resuelven **en paralelo**, preservando el **orden** de la lista de ids.
- Si el detalle de un id similar falla o no existe → se **omite** ese producto, la respuesta sigue siendo 200.
- Lista de similares vacía → 200 con `[]`.
**Pasos:**
1. Implementar el servicio usando solo los puertos (`ProductClientPort`).
2. Manejo de errores por elemento (`onErrorResume`/try-catch por id) para no propagar fallos parciales.
3. Anotar como `@Service` en el módulo `application` (arrastra una dependencia mínima a `spring-context`) o, si se prefiere `application` 100% libre de Spring, registrarlo como `@Bean` en `infrastructure/config` a partir de una clase POJO.
**Criterios de aceptación:**
- Lógica cubierta por tests unitarios (T12) con el puerto mockeado.
- Ninguna llamada HTTP directa desde application.
**Entregable:** caso de uso implementado y testeable en aislamiento.

## Fase 3 — Adaptadores

### T06 — Adaptador REST inbound (controller)
**Descripción:** Controller que implementa la interfaz generada por openapi-generator y delega en el use case.
**Pasos:**
1. `SimilarProductsController implements ProductApi` (interfaz generada).
2. Mapper del modelo de dominio al DTO generado (MapStruct solo si aporta; con un record un mapper manual de 3 líneas basta — código mínimo).
3. Validar `productId` (no vacío; si el contrato lo define como string, validar formato razonable → 400).
**Criterios de aceptación:**
- `GET /product/{id}/similar` responde según el contrato.
- El controller no contiene lógica de negocio, solo mapeo y delegación.
**Entregable:** endpoint funcional en `:5000`.

### T07 — Adaptador HTTP outbound (WebClient)
**Descripción:** Implementación de `ProductClientPort` contra las APIs existentes (`existingApis.yaml`).
**Pasos:**
1. Bean `WebClient` con base URL configurable: `existing-apis.base-url: http://localhost:3001` en `application.yml` (+ variable de entorno para Docker).
2. Implementar `getSimilarIds(productId)` → `GET /product/{id}/similarids` y `getProductDetail(id)` → `GET /product/{id}`.
3. Mapear 404 del upstream a `ProductNotFoundException` (para el producto base) / `Mono.empty()` (para similares).
4. Timeouts de conexión y respuesta explícitos (p. ej. connect 1s, response 2s) — el mock incluye endpoints lentos a propósito.
5. Dimensionar el connection pool de Reactor Netty para la carga del k6 (maxConnections, pendingAcquire).
**Criterios de aceptación:**
- Test con MockWebServer/WireMock cubriendo 200, 404 y timeout.
- Ninguna URL hardcodeada fuera de configuración.
**Entregable:** adaptador HTTP configurado y testeado.

### T08 — Gestión de excepciones (404/400/500)
**Descripción:** Manejo centralizado y consistente de errores hacia el cliente.
**Casos a cubrir:**
| Caso | Respuesta |
|---|---|
| Producto base no encontrado | `404` con cuerpo según contrato |
| `productId` inválido | `400` |
| Error inesperado / upstream caído | `500` controlado (mensaje genérico, sin stacktrace) |
**Pasos:**
1. `@RestControllerAdvice` (o `ErrorWebExceptionHandler` en WebFlux) con handlers para `ProductNotFoundException`, errores de validación y `Exception` genérica.
2. Cuerpo de error uniforme (según defina el contrato; si no define, `{timestamp, status, message}` simple).
3. Log en nivel adecuado (warn para 4xx, error para 5xx) sin filtrar datos sensibles.
**Criterios de aceptación:**
- Los tres casos devuelven el código y cuerpo esperados (verificado en T13).
- Ningún stacktrace llega al cliente.
**Entregable:** handler global + excepciones de dominio.

## Fase 4 — Rendimiento y resiliencia

### T09 — Concurrencia en llamadas a detalles
**Descripción:** Resolver los N detalles de producto en paralelo, no secuencialmente — clave para el test de carga k6.
**Pasos:**
1. `Flux.fromIterable(ids).flatMapSequential(port::getProductDetail, concurrency)` — paralelo pero preservando orden (o `flatMap` + reordenación).
2. Ajustar el nivel de concurrencia (p. ej. 4–10) coherente con el pool de conexiones.
**Criterios de aceptación:**
- Con 5 ids y un upstream de ~100 ms, la respuesta tarda ~1 llamada, no ~5 (verificable en test con delays simulados).
**Entregable:** orquestación paralela medible.

### T10 — Resiliencia: timeouts + circuit breaker
**Descripción:** La app debe degradarse con elegancia cuando el upstream es lento o falla (el mock lo provoca a propósito).
**Pasos:**
1. Añadir el starter de resilience4j compatible con Spring Boot 4.1.x (verificar artifact — `resilience4j-spring-boot3` es la última confirmada a fecha de redacción; puede existir ya un `-spring-boot4` en el momento de implementar) (+ `-reactor` si WebFlux).
2. `TimeLimiter`/`.timeout()` por llamada (p. ej. 2 s) además del response timeout del WebClient.
3. Circuit breaker sobre `getProductDetail` (sliding window ~20, umbral ~50%, wait 5–10 s) — cuando abre, los similares fallidos se omiten en vez de bloquear.
4. Fallback: timeout/error en un detalle → omitir ese producto; error en `similarids` del producto base → 404/500 según el caso.
5. (Opcional, solo si aporta) retry con 1 intento y backoff corto para errores transitorios — cuidado con amplificar carga bajo k6.
**Criterios de aceptación:**
- Test funcional: upstream lento → la respuesta llega dentro del timeout global con los productos disponibles.
- El k6 no produce cascadas de errores 5xx por un endpoint degradado.
**Entregable:** configuración resilience4j en `application.yml` + fallbacks en el adaptador.

### T11 — Caché (Caffeine)
**Descripción:** Cachear respuestas del upstream para absorber la carga del k6 (muchas peticiones repiten los mismos ids).
**Pasos:**
1. Añadir `spring-boot-starter-cache` + `caffeine`.
2. Cachear `getProductDetail(id)` y `getSimilarIds(id)` en el adaptador outbound (la caché es un detalle de infraestructura, no de dominio). En reactivo: `.cache(ttl)` por clave o `CacheMono`/mapa de Caffeine con valores `Mono` cacheados.
3. TTL corto (p. ej. 30–60 s) y tamaño máximo (p. ej. 10 000 entradas) — evitar servir datos rancios indefinidamente.
4. **No** cachear errores (o cachearlos con TTL muy corto) para no fijar fallos transitorios.
**Criterios de aceptación:**
- Segunda petición al mismo id no golpea el upstream (verificable con WireMock verify count).
- Configuración de TTL/tamaño externalizada en `application.yml`.
**Entregable:** caché operativa y configurable.

## Fase 5 — Tests

### T12 — Tests unitarios
**Descripción:** Cobertura unitaria del núcleo y los adaptadores en aislamiento, respetando la separación por módulos: los tests de `domain`/`application` viven en el propio módulo y no dependen de Spring; los de `infrastructure` sí.
**Alcance:**
1. `application/src/test`: `SimilarProductsService` (puerto mockeado con Mockito): happy path, orden preservado, similar fallido omitido, lista vacía, base no encontrado → excepción. `StepVerifier` si es reactivo.
2. `infrastructure/src/test`: adaptador HTTP (MockWebServer): mapeo de 200/404/timeout.
3. `infrastructure/src/test`: handler de excepciones: mapeo excepción → status.
4. `infrastructure/src/test`: mapper dominio↔DTO.
**Criterios de aceptación:**
- `./gradlew test` verde en los tres módulos; el núcleo de negocio (`domain`/`application`) con cobertura alta (~>80%).
- Tests de `domain`/`application` rápidos (<10 s) y sin levantar contexto Spring (ese módulo ni siquiera depende de `spring-context` salvo por la anotación `@Service`, ver T05).
**Entregable:** suite unitaria en `domain/src/test/java`, `application/src/test/java` e `infrastructure/src/test/java`, cada una espejando los paquetes de su propio módulo.

### T13 — Tests funcionales/e2e (≥5 casuísticas)
**Descripción:** Tests de la API completa (app arrancada) con RestAssured + WireMock simulando las APIs existentes. Se valora e2e por encima de integración pura. Viven en `infrastructure/src/test`, único módulo con contexto Spring Boot completo.
**Casuísticas mínimas:**
| # | Escenario | Esperado |
|---|---|---|
| 1 | Producto con similares existentes | `200`, detalles completos y en orden |
| 2 | Producto base inexistente | `404` |
| 3 | Producto sin similares | `200`, `[]` |
| 4 | Un id similar sin detalle (404 upstream) | `200`, ese producto omitido |
| 5 | Upstream lento/timeout en un detalle | `200` degradado dentro del timeout |
| 6 | `productId` inválido | `400` |
**Pasos:**
1. `@SpringBootTest(webEnvironment = RANDOM_PORT)` + WireMock (puerto dinámico, base-url inyectada por propiedad).
2. RestAssured (o `WebTestClient`) contra la app; aserciones de status, cuerpo JSON y orden.
3. Nombrar los tests por escenario (`shouldReturn404WhenProductDoesNotExist`…).
**Criterios de aceptación:**
- ≥5 escenarios verdes ejecutables con `./gradlew check` sin infraestructura externa.
**Entregable:** suite funcional autocontenida.

### T14 — Verificación con docker-compose + k6
**Descripción:** Pasar el test oficial del enunciado contra la app real.
**Pasos:**
1. `docker-compose up -d simulado influxdb grafana` (desde `backendDevTest/`).
2. Verificar mock: `curl http://localhost:3001/product/1/similarids`.
3. Arrancar la app en `:5000` y probar manualmente `curl http://localhost:5000/product/1/similar`.
4. `docker-compose run --rm k6 run scripts/test.js`.
5. Revisar Grafana (`http://localhost:3000/d/Le2Ku9NMk/k6-performance-test`): latencias y tasa de error.
6. Iterar sobre T09–T11 si los números no acompañan.
**Criterios de aceptación:**
- k6 finaliza con tasa de error mínima y latencias estables (sin degradación progresiva).
**Entregable:** captura/notas de resultados k6 para el README.

## Fase 6 — Entrega

### T15 — SwaggerUI
**Descripción:** Exponer el contrato en SwaggerUI para exploración manual.
**Pasos:**
1. Con `springdoc` ya presente (T02), configurar que sirva el YAML del contrato (`springdoc.swagger-ui.url: /api/similarProducts.yaml`) en vez de la introspección, coherente con API-first.
2. Verificar `http://localhost:5000/swagger-ui.html` y probar el endpoint desde la UI.
**Criterios de aceptación:**
- SwaggerUI accesible y ejecuta peticiones reales contra la app.
**Entregable:** UI operativa documentada en el README.

### T16 — Dockerfile de la app
**Descripción:** Imagen Docker de la aplicación para arrancarla con un solo comando.
**Pasos:**
1. `Dockerfile` multi-stage: stage de build con Gradle (wrapper) + JDK que compila todo el proyecto multi-módulo y empaqueta el jar ejecutable de `infrastructure` (`./gradlew :infrastructure:bootJar`); stage final con JRE slim (eclipse-temurin) que copia solo ese jar.
2. Exponer 5000; `EXISTING_APIS_BASE_URL` como variable de entorno (en Docker el mock es `http://host.docker.internal:3001` o red compartida).
3. (Opcional) `docker-compose.override.yml` o compose propio que añada `yourapp` junto al `simulado`.
4. Probar: `docker build` + `docker run -p 5000:5000` + k6 contra el contenedor.
**Criterios de aceptación:**
- La imagen construye desde cero y pasa el k6 igual que en local.
**Entregable:** `Dockerfile` (+ compose opcional) documentado.

### T17 — README
**Descripción:** Documentación de entrega: cómo ejecutar, cómo testear, y decisiones de diseño.
**Contenido mínimo:**
1. Descripción breve del servicio y el contrato.
2. Requisitos (Java 21, Gradle wrapper incluido, Docker).
3. Ejecución: local (`./gradlew bootRun`) y Docker.
4. Tests: unitarios/funcionales (`./gradlew check`) y k6 oficial.
5. Decisiones: arquitectura hexagonal (diagrama simple de paquetes), API-first, resiliencia (timeouts, circuit breaker), caché, y trade-offs asumidos.
**Criterios de aceptación:**
- Una persona sin contexto puede clonar, ejecutar y testear siguiendo solo el README.
**Entregable:** `README.md` en la raíz.

### T18 — Revisión final y push
**Descripción:** Pasada final de calidad antes de entregar.
**Checklist:**
- [ ] Sin código muerto, imports sin uso, TODOs ni comentarios obsoletos.
- [ ] `build.gradle`(`.kts`) de cada módulo (`domain`, `application`, `infrastructure`): cada dependencia justificada; versiones gestionadas por el BOM de Spring Boot (plugin `io.spring.dependency-management` o `platform()`) desde el build raíz.
- [ ] Formato consistente; nombres claros; sin warnings del compilador.
- [ ] `./gradlew clean build` verde desde un clone limpio.
- [ ] k6 pasado una última vez.
- [ ] Historial de commits legible; push final al repo público.
**Criterios de aceptación:** todos los checks marcados.
**Entregable:** repo público final listo para revisión.
