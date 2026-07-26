# Native VPS Deployment with GitHub Actions and Podman Quadlet

The [`.github/workflows/deploy-native-vps.yml`](../../.github/workflows/deploy-native-vps.yml)
workflow compiles Quarkus as a native Linux `amd64` executable, builds a
container image, publishes it to GHCR, and deploys the immutable
`sha-<commit>` tag over SSH. The remote script pulls the image first, restarts
the rootless Quadlet service, checks `/q/openapi`, and restores the previous
version if the health check fails.

The `latest` image is also published, but deployments do not depend on that
mutable tag.

## 1. VPS Requirements

- An `x86_64` Linux server running systemd.
- Podman 5 or later with Quadlet.
- `curl`, `grep`, `sed`, and the standard GNU utilities.
- A deployment user without `sudo`; the examples use `deploy`.
- PostgreSQL reachable from the container.

The native executable targets `linux/amd64`. An ARM64 VPS requires an ARM64
GitHub runner, and the workflow's `--platform` value must also be changed.
Emulating only the final container-image build is not sufficient. The workflow
uses `quarkus.native.march=compatibility` so the resulting executable does not
require the hosted runner CPU's `x86-64-v3` extensions.

As the VPS administrator, allow the user's systemd services to run even when
`deploy` does not have an active login session:

```bash
sudo loginctl enable-linger deploy
```

Then log in directly as that user and prepare the configuration:

```bash
install -d -m 700 ~/.config/containers/systemd
install -m 600 /dev/null \
  ~/.config/containers/systemd/geographic-reference-service.env
```

The file must use the following format:

```dotenv
DB_USERNAME=geographic_reference_service
DB_PASSWORD=a-long-password
DB_REACTIVE_URL=postgresql://database-host:5432/geographic_reference_service
DB_JDBC_URL=jdbc:postgresql://database-host:5432/geographic_reference_service
```

Do not store this file or the PostgreSQL password in GitHub. The versioned
example is available at
[`deploy/quadlet/geographic-reference-service.env.example`](../../deploy/quadlet/geographic-reference-service.env.example).

If PostgreSQL is installed directly on the same VPS, `127.0.0.1` inside the
container does not refer to the host. Use `host.containers.internal` if it is
provided by your distribution's Podman configuration, or connect both
containers to a Podman network and use the PostgreSQL container's DNS name.
Do not expose PostgreSQL on the public IPv4 address.

## 2. Read-Only GHCR Access from the VPS

GHCR packages are initially private in many repositories. Create a classic
personal access token with only the `read:packages` scope and authenticate
Podman once as `deploy`:

```bash
install -d -m 700 ~/.config/containers
read -rsp "GHCR token: " GHCR_READ_TOKEN
printf '%s' "${GHCR_READ_TOKEN}" |
  podman login \
    --authfile ~/.config/containers/auth.json \
    --username YOUR_GITHUB_USERNAME \
    --password-stdin \
    ghcr.io
unset GHCR_READ_TOKEN
chmod 600 ~/.config/containers/auth.json
```

The `--authfile` argument is important: Podman's default Linux authentication
file is stored under `/run` and does not survive a reboot. This login is not
required if the GHCR image is public.

## 3. Dedicated SSH Key

Generate an Ed25519 key exclusively for the workflow and add only its public
key to `~deploy/.ssh/authorized_keys`:

```bash
ssh-keygen -t ed25519 \
  -C github-actions-geographic-reference-service \
  -f github-actions-geographic-reference-service
```

The SSH user does not need `sudo`, Docker group membership, or rootful Podman.
You can prefix this key's line in `authorized_keys` with `restrict` to disable
forwarding, X11, and PTY access without preventing deployment commands.

Obtain the `known_hosts` line and verify its fingerprint against the server's
key from your provider's console before trusting it:

```bash
ssh-keyscan -p 22 YOUR_VPS_IPV4
sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
```

Do not run an unverified `ssh-keyscan` inside the workflow. Doing so removes
the protection against connecting to an impersonated server.

## 4. GitHub Environment and Secrets

Create an environment named `production` under `Settings → Environments`.
Restrict deployments to `main` and, if supported by your plan, require manual
approval.

Configure these secrets inside `production`:

| Name | Value |
| --- | --- |
| `VPS_HOST` | The VPS public IPv4 address |
| `VPS_USER` | The rootless user, for example `deploy` |
| `VPS_SSH_PRIVATE_KEY` | The complete dedicated private key |
| `VPS_SSH_KNOWN_HOSTS` | The verified `known_hosts` line |

Optionally configure the `VPS_SSH_PORT` variable. Port `22` is used when the
variable is absent. GitHub automatically creates the `GITHUB_TOKEN` used to
publish the image to GHCR, so no additional write token is required.

The first push to `main`, or a manual workflow run from `main`, will perform the
deployment.

## 5. Firewall

The Quadlet publishes Quarkus on `127.0.0.1:8080`, so port 8080 is not exposed
on the public IPv4 address. Place Caddy, Nginx, or HAProxy in front of the
service and terminate TLS there.

Recommended inbound rules:

| Port | Source | Purpose |
| --- | --- | --- |
| TCP 22, or your SSH port | GitHub runner or the Internet when using standard hosted runners | SSH deployment |
| TCP 80 | Internet | HTTP-to-HTTPS redirection and/or ACME |
| TCP 443 | Internet | HTTPS API through the reverse proxy |
| TCP 8080 | None | Bound only to loopback |
| TCP 5432 | None from the Internet | PostgreSQL must not be public |

If the database runs on another server, allow port 5432 on **that server** only
from the VPS address or through a private network/VPN.

For outbound traffic, the VPS needs DNS and HTTPS (TCP 443) to pull from GHCR
and GitHub content endpoints. It also needs access to the database port when
the database is remote. A normal allow-outbound policy does not require
additional rules.

Standard GitHub-hosted runners do not have a single static address. GitHub
publishes many address ranges and updates them weekly, so it does not recommend
using those ranges as a firewall allowlist. Available alternatives are:

1. Keep SSH public with key-only authentication, `PasswordAuthentication no`,
   `PermitRootLogin no`, a user without `sudo`, and brute-force protection.
2. Use a dedicated self-hosted runner on the VPS. It initiates outbound
   connections over TCP 443 and removes the need to expose SSH to GitHub.
3. Connect the hosted runner and VPS through a short-lived VPN connection.

For a small VPS, a self-hosted runner dedicated only to the deployment job
provides the lowest network exposure. Keep the native build on a GitHub-hosted
runner. Do not assign the VPS runner to `pull_request` workflows, especially
for a public repository, because workflow code would execute with access to the
server.

## 6. Operations and Troubleshooting

Run these commands as the `deploy` user:

```bash
systemctl --user status geographic-reference-service.service
journalctl --user -u geographic-reference-service.service -f
podman ps
curl --fail http://127.0.0.1:8080/q/openapi
```

The final Quadlet is installed at:

```text
~/.config/containers/systemd/geographic-reference-service.container
```

The deployment script does not delete old images. They are retained so a
rollback can restart the previous image reference.
