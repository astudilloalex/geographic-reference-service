# Despliegue nativo en un VPS con GitHub Actions y Podman Quadlet

El workflow [`.github/workflows/deploy-native-vps.yml`](../../.github/workflows/deploy-native-vps.yml)
compila Quarkus como ejecutable nativo Linux `amd64`, construye una imagen,
la publica en GHCR y despliega por SSH el tag inmutable
`sha-<commit>`. El script remoto descarga primero la imagen, reinicia el
Quadlet rootless, comprueba `/q/openapi` y vuelve a la versión anterior si la
comprobación falla.

La imagen `latest` también se publica, pero el despliegue no depende de ese tag
mutable.

## 1. Requisitos del VPS

- Linux `x86_64` con systemd.
- Podman 5 o posterior con Quadlet.
- `curl`, `grep`, `sed` y las utilidades básicas de GNU.
- Un usuario de despliegue sin `sudo`; en los ejemplos se llama `deploy`.
- PostgreSQL accesible desde el contenedor.

El ejecutable nativo se genera para `linux/amd64`. Para un VPS ARM64 se necesita
un runner de GitHub ARM64 y se debe cambiar también `--platform` en el workflow.
No basta con emular solamente la última construcción de la imagen.
El workflow usa `quarkus.native.march=compatibility` para no exigir las
extensiones `x86-64-v3` de la CPU del runner hospedado.

Como administrador del VPS, habilita el servicio systemd de usuario incluso
cuando `deploy` no tenga una sesión abierta:

```bash
sudo loginctl enable-linger deploy
```

Después entra directamente como ese usuario y prepara la configuración:

```bash
install -d -m 700 ~/.config/containers/systemd
install -m 600 /dev/null \
  ~/.config/containers/systemd/geographic-reference-service.env
```

El archivo debe tener este formato:

```dotenv
DB_USERNAME=geographic_reference_service
DB_PASSWORD=una-clave-larga
DB_REACTIVE_URL=postgresql://database-host:5432/geographic_reference_service
DB_JDBC_URL=jdbc:postgresql://database-host:5432/geographic_reference_service
```

No guardes este archivo ni la contraseña de PostgreSQL en GitHub. El ejemplo
versionado está en
[`deploy/quadlet/geographic-reference-service.env.example`](../../deploy/quadlet/geographic-reference-service.env.example).

Si PostgreSQL está instalado directamente en el mismo VPS, `127.0.0.1` dentro
del contenedor no apunta al host. Usa `host.containers.internal` si la
configuración de Podman de tu distribución lo proporciona, o conecta ambos
contenedores a una red Podman y usa el nombre DNS del contenedor de PostgreSQL.
No publiques PostgreSQL en la IPv4 pública.

## 2. Acceso de sólo lectura a GHCR desde el VPS

Los paquetes GHCR son privados inicialmente en muchos repositorios. Crea un
personal access token clásico con solamente `read:packages` y autentica Podman
una vez como `deploy`:

```bash
install -d -m 700 ~/.config/containers
read -rsp "GHCR token: " GHCR_READ_TOKEN
printf '%s' "${GHCR_READ_TOKEN}" |
  podman login \
    --authfile ~/.config/containers/auth.json \
    --username TU_USUARIO_GITHUB \
    --password-stdin \
    ghcr.io
unset GHCR_READ_TOKEN
chmod 600 ~/.config/containers/auth.json
```

Es importante indicar `--authfile`: el archivo predeterminado de Podman en
Linux vive bajo `/run` y no sobrevive a un reinicio. Si haces pública la imagen
GHCR, este login no es necesario.

## 3. Clave SSH dedicada

Genera una clave Ed25519 específica para el workflow y añade únicamente la
clave pública a `~deploy/.ssh/authorized_keys`:

```bash
ssh-keygen -t ed25519 \
  -C github-actions-geographic-reference-service \
  -f github-actions-geographic-reference-service
```

El usuario SSH no necesita `sudo`, pertenecer al grupo de Docker ni ejecutar
Podman como root. Puedes anteponer `restrict` a la línea de esta clave en
`authorized_keys` para desactivar forwarding, X11 y PTY sin impedir los
comandos de despliegue.

Obtén la línea de `known_hosts` y verifica su huella contra la clave del servidor
desde la consola de tu proveedor antes de confiar en ella:

```bash
ssh-keyscan -p 22 TU_IPV4
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

No uses `ssh-keyscan` dentro del workflow sin verificar la huella: eso elimina
la protección frente a un servidor impostor.

## 4. Environment y secretos de GitHub

En `Settings → Environments`, crea el environment `production`. Restringe el
despliegue a `main` y, si tu plan lo permite, exige aprobación manual.

Configura estos secrets dentro de `production`:

| Nombre | Contenido |
| --- | --- |
| `VPS_HOST` | IPv4 pública del VPS |
| `VPS_USER` | Usuario rootless, por ejemplo `deploy` |
| `VPS_SSH_PRIVATE_KEY` | Contenido completo de la clave privada dedicada |
| `VPS_SSH_KNOWN_HOSTS` | Línea verificada de `known_hosts` |

Configura opcionalmente la variable `VPS_SSH_PORT`. Si no existe, se usa `22`.
`GITHUB_TOKEN` publica la imagen en GHCR y GitHub lo genera automáticamente; no
hay que crear otro secreto de escritura.

El primer `push` a `main`, o la ejecución manual del workflow desde `main`,
realizará el despliegue.

## 5. Firewall

El Quadlet publica Quarkus como `127.0.0.1:8080`, de modo que el puerto 8080 no
queda expuesto en la IPv4 pública. Coloca Caddy, Nginx o HAProxy delante y
termina TLS allí.

Reglas de entrada recomendadas:

| Puerto | Origen | Motivo |
| --- | --- | --- |
| TCP 22, o tu puerto SSH | GitHub runner o Internet si usas runners hospedados estándar | Despliegue SSH |
| TCP 80 | Internet | Redirección HTTP a HTTPS y/o ACME |
| TCP 443 | Internet | API HTTPS mediante reverse proxy |
| TCP 8080 | Ninguno | Está enlazado solamente a loopback |
| TCP 5432 | Ninguno desde Internet | PostgreSQL no debe ser público |

Si la base de datos vive en otro servidor, permite 5432 en **ese servidor**
solamente desde la IP del VPS o a través de una red privada/VPN.

Para salida, el VPS necesita DNS y HTTPS (TCP 443) para descargar desde GHCR y
los endpoints de contenido de GitHub, además del puerto de la base de datos
cuando ésta sea remota. Con una política normal de salida permitida no hace
falta añadir reglas especiales.

Los runners estándar de GitHub no tienen una única IP fija. GitHub publica
muchos rangos y los actualiza semanalmente, por lo que no recomienda usarlos
como allowlist del firewall. Hay tres alternativas:

1. Mantener SSH público, sólo con clave, `PasswordAuthentication no`,
   `PermitRootLogin no`, usuario sin sudo y protección contra intentos.
2. Usar un runner self-hosted dedicado en el VPS. Éste inicia conexiones
   salientes por TCP 443 y elimina la necesidad de abrir SSH para GitHub.
3. Conectar el runner hospedado y el VPS mediante una VPN de corta duración.

Para un VPS pequeño, un runner self-hosted dedicado sólo al job de despliegue es
la opción con menor exposición de red; mantén la compilación nativa en el runner
hospedado. No asignes ese runner a workflows de `pull_request`, especialmente en
un repositorio público, porque el código del workflow se ejecuta con acceso al
servidor.

## 6. Operación y diagnóstico

Ejecuta estos comandos como el usuario `deploy`:

```bash
systemctl --user status geographic-reference-service.service
journalctl --user -u geographic-reference-service.service -f
podman ps
curl --fail http://127.0.0.1:8080/q/openapi
```

El Quadlet final se instala en:

```text
~/.config/containers/systemd/geographic-reference-service.container
```

El script no elimina imágenes antiguas: se conservan para que el rollback pueda
reiniciar la referencia anterior.
