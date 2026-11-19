# Guía de despliegue

Arquitectura de producción:

```
Navegador ──HTTPS──> Vercel (frontend React)
    │
    └──HTTPS/JSON + JWT──> Render (API Spring Boot en Docker, región Virginia)
                                 │
                                 └──TLS──> MongoDB Atlas (AWS us-east-1)
```

## 1. MongoDB Atlas

1. Crear un clúster gratuito **M0** en AWS, región **N. Virginia (us-east-1)**.
2. En *Database Access*, crear un usuario de base de datos con contraseña.
3. En *Network Access*, permitir `0.0.0.0/0` (Render no tiene IP fija en el plan gratuito).
4. En *Connect → Drivers*, copiar la cadena de conexión y agregar el nombre de la base:
   `mongodb+srv://<usuario>:<contraseña>@<cluster>.mongodb.net/rastreo_iot?retryWrites=true&w=majority`

La cadena de conexión es un secreto: solo se guarda en la variable `MONGODB_URI` de Render, nunca en el repositorio.

## 2. API en Render

1. En Render: *New → Blueprint* y elegir este repositorio. Render lee `render.yaml` y crea el servicio `rastreo-iot-api` (Docker, carpeta `backend`, región Virginia, plan Free).
   - Alternativa manual: *New → Web Service*, runtime **Docker**, *Root Directory* `backend`, *Health Check Path* `/api/salud`.
2. En *Environment*, definir `MONGODB_URI` con la cadena de Atlas. `JWT_SECRETO` y `CLAVE_DISPOSITIVOS` se generan solos con el blueprint.
3. Esperar el primer despliegue (compila con Maven dentro de Docker, tarda unos minutos).
4. Verificar:
   - `https://<servicio>.onrender.com/api/salud` responde `{"estado":"en línea"}`.
   - `https://<servicio>.onrender.com/swagger-ui.html` muestra la documentación.

En el plan gratuito el servicio se duerme tras 15 minutos sin uso; la primera petición después tarda unos 50 segundos en responder.

## 3. Frontend en Vercel

1. En Vercel: *Add New → Project* e importar este repositorio.
2. *Root Directory*: `frontend`. Vercel detecta Vite (build `npm run build`, salida `dist`).
3. En *Environment Variables*, definir `VITE_API_URL` con la URL de Render, sin barra final. Ejemplo: `https://rastreo-iot-api.onrender.com`.
4. Desplegar. `vercel.json` redirige todas las rutas a `index.html` para que funcionen los enlaces directos (por ejemplo `/monitoreo`).

Si se usa un dominio propio distinto de `*.vercel.app`, agregarlo a `CORS_ORIGENES` en Render.

## 4. Prueba rápida

1. Abrir la URL de Vercel e iniciar sesión con una cuenta de demostración (ver README).
2. En *Monitoreo* deben aparecer los activos moviéndose; el simulador envía datos cada 20 segundos.
3. Para enviar telemetría manual (por ejemplo desde Postman):

```http
POST https://<servicio>.onrender.com/api/telemetria
X-Clave-Dispositivo: <valor de CLAVE_DISPOSITIVOS en Render>
Content-Type: application/json

{ "serial": "GPS-4C1E-77A2", "latitud": 4.6600, "longitud": -74.0800, "velocidad": 95, "nivelBateria": 80 }
```
