# Similar Products API

Servicio que, dado un `productId`, devuelve el detalle de sus productos similares
(`GET /product/{productId}/similar`), agregando dos llamadas a las APIs existentes:

1. `GET /product/{productId}/similarids` → lista de ids similares.
2. `GET /product/{id}` (una por cada id similar) → detalle de cada producto.

Los ids cuyo detalle falla (404, error, timeout) se omiten de la respuesta en vez de
hacer fallar la petición completa. El contrato de la API expuesta está definido
API-first en [`infrastructure/src/main/resources/api/similarProducts.yaml`](infrastructure/src/main/resources/api/similarProducts.yaml).

## Requisitos

- Java 21 (el wrapper de Gradle se encarga de descargar Gradle 9.6.1; no hace falta
  tenerlo instalado).
- Docker (para levantar el contenedor de la app y/o el mock de las APIs existentes).

## Ejecución

### Local (`bootRun`)

```bash
./gradlew :infrastructure:bootRun
```

Arranca en `http://localhost:5000`. Por defecto asume que las APIs existentes están
en `http://localhost:3001` (mock); para apuntar a otra URL:

```bash
EXISTING_APIS_BASE_URL=http://localhost:3001 ./gradlew :infrastructure:bootRun
```

### Docker

```bash
docker build -t similar-products .
docker run -p 5000:5000 -e EXISTING_APIS_BASE_URL=http://host.docker.internal:3001 similar-products
```

El `Dockerfile` es multi-stage: una etapa con `eclipse-temurin:21-jdk` compila el
proyecto multi-módulo y empaqueta el jar ejecutable de `infrastructure`
(`./gradlew :infrastructure:bootJar`); la etapa final, sobre `eclipse-temurin:21-jre`,
solo copia ese jar. Si el mock de las APIs existentes (`simulado`) corre también en
Docker, sustituye `host.docker.internal` por el nombre de servicio en la red compartida
de su `docker-compose`.

### Documentación interactiva (Swagger UI)

Con la app arrancada (local o Docker):

- `http://localhost:5000/swagger-ui.html` — UI interactiva sobre el contrato real
  (`/api/similarProducts.yaml`).
- `http://localhost:5000/v3/api-docs` — spec generado por springdoc.

## Tests

```bash
./gradlew check
```

Ejecuta, por módulo:

- **`application`**: `SimilarProductsServiceTest` — orquestación del caso de uso
  (concurrencia, omisión de ids fallidos).
- **`infrastructure`**:
  - `ProductHttpClientAdapterTest` — adaptador WebClient hacia las APIs existentes.
  - `ProductDetailMapperTest` — mapeo dominio → DTO del contrato.
  - `SimilarProductsControllerTest` — validación de `productId` y delegación al caso de uso.
  - `GlobalExceptionHandlerTest` — mapeo de excepciones a 400/404/500.
  - `SimilarProductsE2ETest` — 6 escenarios end-to-end (`@SpringBootTest` + WireMock +
    `WebTestClient`): éxito con varios similares, similar con detalle 404 (se omite),
    similar cuyo detalle da error 500 (se omite), timeout de detalle (se omite),
    `productId` inexistente (404) y `productId` vacío/en blanco (400).

`domain` no tiene tests propios: solo contiene el modelo (`ProductDetail`, un record) y
una excepción, sin lógica de negocio que testear.

### Prueba de carga (k6)

Verificado con el `docker-compose`/scripts de k6 provistos en el enunciado
(`backendDevTest/`), contra la app corriendo tanto en local (`bootRun`) como en el
contenedor Docker descrito arriba. Resultados y análisis completos en
[`plan/k6-results.md`](plan/k6-results.md).

## Decisiones de diseño

### Arquitectura hexagonal

Tres módulos Gradle con dependencias en una sola dirección:

```
infrastructure → application → domain
```

- **`domain`**: modelo (`ProductDetail`) y puertos —
  `GetSimilarProductsUseCase` (in), `ProductClientPort` (out). Sin Spring, sin código
  generado.
- **`application`**: implementa el caso de uso (`SimilarProductsService`) orquestando
  el puerto `ProductClientPort`. Sin dependencias web ni de cliente HTTP concreto.
- **`infrastructure`**: adaptadores de entrada (`SimilarProductsController`, generado
  a partir del contrato con openapi-generator) y de salida (`ProductHttpClientAdapter`,
  sobre `WebClient`), configuración de Spring Boot, manejo de errores y resiliencia.

La regla de dependencias se refuerza con el propio grafo de Gradle: `application` solo
declara `implementation project(':domain')`, e `infrastructure` solo
`implementation project(':application')` (que arrastra `domain` transitivamente).
Ningún módulo depende "hacia arriba".

### API-first

El contrato (`similarProducts.yaml`) es la fuente de verdad: `openapi-generator`
genera la interfaz del controller (`interfaceOnly=true`) y los modelos a partir de él
en tiempo de build; `SimilarProductsController` la implementa. Swagger UI sirve el
mismo YAML estático (`/api/similarProducts.yaml`), no una copia derivada del código.

### Concurrencia

Los detalles de los productos similares se piden en paralelo
(`flatMapSequential(..., concurrency = 8)`), preservando el orden de similitud del
listado original, en vez de secuencialmente uno a uno.

### Resiliencia

- **Timeout** (`resilience4j-timelimiter`, 2s) y **circuit breaker**
  (`resilience4j-circuitbreaker`, ventana de 20 llamadas, umbral de fallo 50%,
  espera en abierto 5s) aplicados a la llamada de detalle por producto
  (`getProductDetail`), no a la llamada base de ids similares.
- Cualquier fallo de detalle (404, 5xx, timeout, circuito abierto) se resuelve con
  `onErrorResume` descartando ese id, en vez de propagar el error a toda la petición:
  la respuesta incluye los similares disponibles aunque algún detalle individual falle.

### Caché

**Pendiente** (tarea T11 del plan, no implementada en esta entrega): la idea es
cachear `getSimilarIds`/`getProductDetail` (p. ej. con Caffeine, TTL corto) para
reducir la carga sobre las APIs existentes y mitigar el pico de errores por
saturación del pool de conexiones observado en la prueba de carga inicial (ver
`plan/k6-results.md`).

### Gestión de errores

Un `@RestControllerAdvice` (`GlobalExceptionHandler`) centraliza el mapeo a la
respuesta HTTP, con un cuerpo uniforme `{timestamp, status, message}` (el contrato no
define un esquema de error propio):

| Origen                                             | HTTP |
|-----------------------------------------------------|------|
| `ProductNotFoundException`                          | 404  |
| `productId` vacío/en blanco, request inválido        | 400  |
| Error inesperado / upstream caído                    | 500  |

### Trade-offs asumidos

- No hay reintentos (`retry`) sobre la llamada de detalle, para no amplificar carga
  sobre las APIs existentes bajo el circuit breaker ya activo.
- El mensaje de error 500 es fijo y no expone detalles internos de la excepción.
- Sin caché (ver arriba): cada petición vuelve a golpear las APIs existentes.
