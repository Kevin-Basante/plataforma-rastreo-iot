# Plataforma de Rastreo IoT

Sistema web para rastrear vehículos, familiares y mascotas con dispositivos IoT (GPS, pulseras y collares). Muestra la posición en tiempo real sobre un mapa, genera alertas de geocerca, SOS y exceso de velocidad, permite enviar órdenes remotas al motor, consultar recorridos, exportar informes PDF y agrupar vehículos en sub-flotas.

Proyecto final de **Diseño de Software**: implementa las 18 historias de usuario de `docs/historias-usuario`.

| Capa | Tecnología | Despliegue |
| --- | --- | --- |
| Frontend | React 18 + Vite, React Router, Leaflet (OpenStreetMap / Esri) | Vercel |
| Backend | Java 21, Spring Boot 3.3 (Web, Security + JWT, Validation, Data MongoDB), springdoc OpenAPI, OpenPDF | Render (Docker) |
| Base de datos | MongoDB | MongoDB Atlas |

## Estructura del repositorio

```
backend/    API REST en Spring Boot (arquitectura por capas)
frontend/   Aplicación web en React
docs/       Historias de usuario, mockups, diagramas, códigos QR y guía de despliegue
render.yaml Blueprint de Render para la API
```

### Capas del backend (`backend/src/main/java/com/rastreoiot`)

| Paquete | Responsabilidad |
| --- | --- |
| `presentacion` | Controladores REST y manejo uniforme de errores (`{estado, mensaje, detalles, fecha}`) |
| `negocio` | Servicios con las reglas de las HU, DTO y excepciones |
| `datos` | Repositorios `MongoRepository` |
| `modelo` | Entidades del dominio (`Activo` abstracto → `Vehiculo`, `Familiar`, `Mascota`, etc.) |
| `seguridad` | Filtro JWT, contraseñas con BCrypt y control de acceso por rol |
| `configuracion` | OpenAPI, datos de demostración y simulador de telemetría |

## Ejecutar en local

Requisitos: Java 21, Maven 3.9, Node 20+ y un MongoDB (local o Atlas).

```bash
# 1. Base de datos local (opcional si usas Atlas)
docker run -d --name mongo -p 27017:27017 mongo:7

# 2. API  →  http://localhost:8080  (Swagger en /swagger-ui.html)
cd backend
mvn spring-boot:run
#   con Atlas:  MONGODB_URI="mongodb+srv://..." mvn spring-boot:run

# 3. Frontend  →  http://localhost:5173
cd frontend
cp .env.example .env      # VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Pruebas del backend: `cd backend && mvn test`.

### Variables de entorno de la API

| Variable | Uso | Valor por defecto |
| --- | --- | --- |
| `MONGODB_URI` | Cadena de conexión a MongoDB | `mongodb://localhost:27017/rastreo_iot` |
| `JWT_SECRETO` | Clave para firmar los tokens (mín. 32 caracteres) | clave de desarrollo |
| `CLAVE_DISPOSITIVOS` | Clave que envían los dispositivos en `X-Clave-Dispositivo` | `clave-dispositivos-local` |
| `CORS_ORIGENES` | Orígenes permitidos, separados por coma | `http://localhost:5173,https://*.vercel.app` |
| `DATOS_DEMO` | Carga datos de ejemplo si la base está vacía | `true` |
| `CONTRASENA_DEMO` | Contraseña de las cuentas de ejemplo | `Rastreo2026` |
| `SIMULADOR` | Simula telemetría de los dispositivos cada 20 s | `true` |

## Cuentas de demostración

Se crean automáticamente la primera vez que la API arranca con la base vacía. Todas usan la contraseña `Rastreo2026`.

| Rol | Correo | Qué tiene |
| --- | --- | --- |
| SuperAdmin | `soporte@rastreoiot.co` | Acceso a todo el sistema y a Usuarios y roles |
| Admin de Flota B2B | `andres.gomez@transportesandinos.co` | Transportes Andinos S.A.S.: 14 vehículos y 3 sub-flotas |
| Admin de Flota B2B | `operaciones@logisticasabana.co` | Logística Sabana Ltda.: vehículo PQR-733 |
| Usuario Hogar B2C | `laura.martinez@correo.co` | Vehículo KLM-482, familiar Don Carlos y mascota Luna |

Dispositivos del catálogo sin enlazar (para probar HU-04): `GPS-5E21-0B7C`, `PUL-91C0-3D5E`, `COL-6A4F-1D93` y `GPS-8B04-6C2D`. Sus códigos QR (`RIOT-<serial>`) están en `docs/qr-dispositivos/`.

## Historias de usuario y pantallas

| HU | Funcionalidad | Pantalla | Endpoints principales |
| --- | --- | --- | --- |
| HU-01 | Registro e inicio de sesión seguro | Acceso | `POST /api/auth/registro`, `POST /api/auth/login` |
| HU-02 | Permisos por rol | Menú por rol, Usuarios y roles | `GET/POST /api/usuarios` |
| HU-03 | Datos personales y preferencias | Mi perfil | `PUT /api/usuarios/me`, `PUT /api/usuarios/me/foto` |
| HU-04 | Enlace de dispositivos por QR o serial | Enlazar dispositivo | `POST /api/dispositivos/enlazar` |
| HU-05 | Registro del activo monitoreado | Activos monitoreados | `POST /api/activos` |
| HU-06 | Conexión y batería del dispositivo | Dispositivos, Monitoreo | `GET /api/dispositivos` |
| HU-07 | Posición en tiempo real y velocidad | Monitoreo | `GET /api/monitoreo/ubicaciones` |
| HU-08 | Estado del motor y kilometraje | Monitoreo | `GET /api/monitoreo/ubicaciones` |
| HU-09 | Cambio de vista del mapa | Monitoreo | `GET /api/monitoreo/capas/{capa}` |
| HU-10 | Zonas de seguridad (geocercas) | Geocercas | `POST /api/geocercas` |
| HU-11 | Activar, desactivar y silenciar geocercas | Geocercas | `PUT /api/geocercas/{id}/...` |
| HU-12 | Alertas por geocerca | Centro de alertas | `GET /api/alertas` |
| HU-13 | Señal de auxilio SOS | Banner SOS, Centro de alertas | `POST /api/telemetria/sos` |
| HU-14 | Apagado o bloqueo remoto del motor | Control remoto del motor | `POST /api/ordenes` |
| HU-15 | Alerta por exceso de velocidad | Centro de alertas, Dispositivos | `PUT /api/dispositivos/{id}/limite-velocidad` |
| HU-16 | Recorrido por rango de fechas | Recorrido | `GET /api/monitoreo/dispositivos/{id}/recorrido` |
| HU-17 | Exportación de informes PDF | Informes PDF | `GET /api/informes/dispositivos/{id}/pdf` |
| HU-18 | Sub-flotas o sucursales | Sub-flotas | `GET/POST /api/subflotas` |

La documentación completa de la API está en Swagger: `/swagger-ui.html`.

## Valores definidos para el sistema

Las HU no fijan algunos umbrales; se usaron estos:

- Un dispositivo se considera **desconectado** si no envía datos en 5 minutos.
- **Batería baja**: 20 % o menos.
- **Geocercas** circulares con radio entre 20 m y 50 km; el silencio dura de 1 a 1440 minutos.
- **Límite de velocidad** entre 5 y 250 km/h; si no está definido, el sistema muestra "configuración pendiente".
- El **recorrido** se consulta en rangos de hasta 31 días.
- **Contraseñas**: mínimo 8 caracteres, con una mayúscula y un número. Se guardan con BCrypt.
- No hay hardware real: la telemetría la genera un simulador dentro de la API (o cualquier cliente que llame `POST /api/telemetria` con la clave de dispositivos).

## Despliegue

Los pasos para publicar la API en Render, la base en MongoDB Atlas y el frontend en Vercel están en [docs/despliegue.md](docs/despliegue.md).
