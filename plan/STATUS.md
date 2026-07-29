# Estado de tareas

> Estados: ⬜ Pendiente · 🔵 En progreso · ✅ Completada · ⛔ Bloqueada
> Detalle de cada tarea en [TASKS.md](./TASKS.md).

| ID  | Tarea                                              | Fase        | Estado      | Comentario |
|-----|----------------------------------------------------|-------------|-------------|------------|
| T01 | Inicializar repo Git público                       | Setup       | ✅ Completada |            |
| T02 | Proyecto Spring Boot + dependencias mínimas        | Setup       | ✅ Completada | springdoc-openapi-starter-webflux-ui aplazado a T15 (esperando confirmar versión estable compatible con Spring Boot 4.1/Spring Framework 7) |
| T03 | API-first: contrato + openapi-generator            | Setup       | ✅ Completada | Generator `spring` produce `DefaultApi` (sin `x-tags` en el YAML no hay nombre de tag propio); `openApiNullable=false` para evitar dependencia extra de jackson-databind-nullable |
| T04 | Estructura hexagonal (paquetes y puertos)          | Arquitectura| ✅ Completada | Creado README.md con la regla de dependencias entre módulos (T04 paso 4); contenido completo de README queda para T17 |
| T05 | Dominio y caso de uso SimilarProducts              | Arquitectura| ✅ Completada | `domain` build.gradle: `reactor-core` pasado a `api` (antes `implementation`) y plugin raíz `java`→`java-library`, ya que las firmas de los puertos exponen `Flux`/`Mono` a `application`; añadido `junit-platform-launcher` como `testRuntimeOnly` (requerido por Gradle 9) |
| T06 | Adaptador REST inbound (controller)                | Adaptadores | ✅ Completada | `application/build.gradle`: `domain` pasado a `api` (antes `implementation`), ya que el controller en `infrastructure` necesita ver los puertos y el modelo de dominio; validación de `productId` vacío devuelve 400 vía `ResponseStatusException` directamente (el `@RestControllerAdvice` de T08 cubrirá `ProductNotFoundException` y errores genéricos). Arranque real del endpoint (`bootRun`)/tests e2e quedan pendientes de T07 (aún no hay bean para `ProductClientPort`) |
| T07 | Adaptador HTTP outbound (WebClient)                | Adaptadores | ✅ Completada | `getSimilarIds` decodifica con `bodyToMono(ParameterizedTypeReference<List<String>>)` + `flatMapMany` en vez de `bodyToFlux(String.class)`, ya que Jackson2JsonDecoder no fragmenta un array JSON de escalares en elementos de Flux |
| T08 | Gestión de excepciones (404/400/500)               | Adaptadores | ✅ Completada | `@RestControllerAdvice` en `infrastructure` mapea `ProductNotFoundException`→404, `ResponseStatusException`/`ServerWebInputException`→400 (u otro 4xx), y cualquier 5xx/`Exception` genérica→500 con mensaje fijo (sin detalles de la excepción); cuerpo uniforme `{timestamp, status, message}` al no definir el contrato un esquema de error. Verificación end-to-end formal queda para T13 (arranque real bloqueado por bug preexistente de component-scan entre módulos, ajeno a esta tarea); se añadió test unitario del handler en aislamiento para validar el mapeo mientras tanto |
| T09 | Concurrencia en llamadas a detalles                | Perf/Resil. | ⬜ Pendiente |            |
| T10 | Resiliencia: timeouts + circuit breaker            | Perf/Resil. | ⬜ Pendiente |            |
| T11 | Caché (Caffeine)                                   | Perf/Resil. | ⬜ Pendiente |            |
| T12 | Tests unitarios                                    | Tests       | ⬜ Pendiente |            |
| T13 | Tests funcionales/e2e (≥5 casuísticas)             | Tests       | ⬜ Pendiente |            |
| T14 | Verificación con docker-compose + k6               | Tests       | ⬜ Pendiente |            |
| T15 | SwaggerUI                                          | Entrega     | ⬜ Pendiente |            |
| T16 | Dockerfile de la app                               | Entrega     | ⬜ Pendiente |            |
| T17 | README (ejecución, tests, decisiones)              | Entrega     | ⬜ Pendiente |            |
| T18 | Revisión final y push                              | Entrega     | ⬜ Pendiente |            |

**Progreso global:** 8 / 18 tareas completadas
