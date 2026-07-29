# Similar Products API

## Arquitectura

El proyecto está organizado como tres módulos Gradle independientes que implementan
arquitectura hexagonal:

```
infrastructure → application → domain
```

- **`domain`**: modelo de dominio (`ProductDetail`) y puertos (`GetSimilarProductsUseCase`,
  `ProductClientPort`). Sin dependencias de Spring ni de código generado.
- **`application`**: casos de uso (implementa los puertos `in` de `domain`). Sin
  dependencias web (WebFlux/WebMVC) ni clientes HTTP.
- **`infrastructure`**: adaptadores de entrada (REST, a partir del contrato
  `similarProducts.yaml`) y de salida (cliente HTTP hacia las APIs existentes), más
  configuración de Spring Boot.

La regla de dependencias se refuerza con el propio grafo de Gradle: `application` solo
declara `implementation project(':domain')`, e `infrastructure` solo
`implementation project(':application')` (que arrastra `domain` transitivamente). Ningún
módulo depende "hacia arriba".
