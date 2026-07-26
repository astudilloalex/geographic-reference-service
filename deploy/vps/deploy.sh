#!/usr/bin/env bash
set -Eeuo pipefail

readonly APP_NAME="geographic-reference-service"
readonly SERVICE_NAME="${APP_NAME}.service"
readonly IMAGE_REFERENCE="${1:?Usage: deploy.sh IMAGE_REFERENCE [SOURCE_DIRECTORY]}"
readonly SOURCE_DIRECTORY="${2:-${HOME}/.local/share/${APP_NAME}}"
readonly CONFIG_HOME="${XDG_CONFIG_HOME:-${HOME}/.config}"
readonly QUADLET_DIRECTORY="${CONFIG_HOME}/containers/systemd"
readonly QUADLET_TEMPLATE="${SOURCE_DIRECTORY}/${APP_NAME}.container.template"
readonly QUADLET_FILE="${QUADLET_DIRECTORY}/${APP_NAME}.container"
readonly ENVIRONMENT_FILE="${QUADLET_DIRECTORY}/${APP_NAME}.env"
readonly REGISTRY_AUTH_FILE="${CONFIG_HOME}/containers/auth.json"
readonly HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:8081/q/openapi}"
readonly -a REQUIRED_NETWORKS=("internal-services" "geographic-db")

export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
export DBUS_SESSION_BUS_ADDRESS="${DBUS_SESSION_BUS_ADDRESS:-unix:path=${XDG_RUNTIME_DIR}/bus}"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

show_service_logs() {
  journalctl --user --unit "${SERVICE_NAME}" --lines 80 --no-pager >&2 || true
}

for required_command in curl grep install journalctl mktemp podman sed stat systemctl uname; do
  command -v "${required_command}" >/dev/null 2>&1 ||
    fail "Missing required command on the VPS: ${required_command}"
done

case "$(uname -m)" in
  x86_64 | amd64) ;;
  *)
    fail "This workflow publishes linux/amd64 images; the VPS architecture is $(uname -m)"
    ;;
esac

if [[ ! "${IMAGE_REFERENCE}" =~ ^ghcr\.io/[a-z0-9._-]+/[a-z0-9._/-]+:sha-[0-9a-f]{40}$ ]]; then
  fail "Unexpected image reference: ${IMAGE_REFERENCE}"
fi

[[ -f "${QUADLET_TEMPLATE}" ]] ||
  fail "Quadlet template not found: ${QUADLET_TEMPLATE}"

install -d -m 700 "${QUADLET_DIRECTORY}"

[[ -f "${ENVIRONMENT_FILE}" ]] ||
  fail "Create ${ENVIRONMENT_FILE} with the production database variables before the first deployment"

environment_mode="$(stat -c '%a' "${ENVIRONMENT_FILE}")"
if (( (8#${environment_mode} & 077) != 0 )); then
  fail "${ENVIRONMENT_FILE} contains secrets and must not be accessible by group or others (use chmod 600)"
fi

missing_variables=()
for variable_name in DB_USERNAME DB_PASSWORD DB_REACTIVE_URL DB_JDBC_URL; do
  if ! grep -Eq "^${variable_name}=.+" "${ENVIRONMENT_FILE}"; then
    missing_variables+=("${variable_name}")
  fi
done

if (( ${#missing_variables[@]} > 0 )); then
  fail "Missing production variables in ${ENVIRONMENT_FILE}: ${missing_variables[*]}"
fi

missing_networks=()
for network_name in "${REQUIRED_NETWORKS[@]}"; do
  if ! podman network exists "${network_name}"; then
    missing_networks+=("${network_name}")
  fi
done

if (( ${#missing_networks[@]} > 0 )); then
  fail "Required Podman networks are missing for user $(id -un): ${missing_networks[*]}. Install and start them from the VPS infrastructure configuration"
fi

pull_options=()
if [[ -f "${REGISTRY_AUTH_FILE}" ]]; then
  registry_auth_mode="$(stat -c '%a' "${REGISTRY_AUTH_FILE}")"
  if (( (8#${registry_auth_mode} & 077) != 0 )); then
    fail "${REGISTRY_AUTH_FILE} must not be accessible by group or others (use chmod 600)"
  fi
  pull_options+=(--authfile "${REGISTRY_AUTH_FILE}")
fi

printf 'Pulling %s before restarting the service...\n' "${IMAGE_REFERENCE}"
if ! podman pull "${pull_options[@]}" "${IMAGE_REFERENCE}"; then
  fail "Unable to pull the image. If GHCR is private, log in with podman using ${REGISTRY_AUTH_FILE}"
fi

new_quadlet="$(mktemp "${QUADLET_DIRECTORY}/.${APP_NAME}.new.XXXXXX")"
previous_quadlet="$(mktemp)"
had_previous_quadlet=false

cleanup() {
  rm -f "${new_quadlet}" "${previous_quadlet}"
}
trap cleanup EXIT

if [[ -f "${QUADLET_FILE}" ]]; then
  cp "${QUADLET_FILE}" "${previous_quadlet}"
  had_previous_quadlet=true
fi

sed "s|@@IMAGE@@|${IMAGE_REFERENCE}|g" "${QUADLET_TEMPLATE}" > "${new_quadlet}"
install -m 600 "${new_quadlet}" "${QUADLET_FILE}"

rollback() {
  printf 'Rolling back the Quadlet configuration...\n' >&2

  if [[ "${had_previous_quadlet}" == true ]]; then
    install -m 600 "${previous_quadlet}" "${QUADLET_FILE}"
    systemctl --user daemon-reload
    systemctl --user restart "${SERVICE_NAME}" || true
  else
    rm -f "${QUADLET_FILE}"
    systemctl --user daemon-reload
    systemctl --user stop "${SERVICE_NAME}" || true
  fi
}

systemctl --user daemon-reload

if ! systemctl --user cat "${SERVICE_NAME}" >/dev/null; then
  rollback
  fail "Quadlet could not generate ${SERVICE_NAME}; inspect the Podman generator output on the VPS"
fi

if ! systemctl --user restart "${SERVICE_NAME}"; then
  show_service_logs
  rollback
  fail "The new Quadlet service did not start"
fi

healthy=false
for ((attempt = 1; attempt <= 60; attempt++)); do
  if curl --fail --silent --show-error --max-time 2 "${HEALTHCHECK_URL}" >/dev/null; then
    healthy=true
    break
  fi
  sleep 1
done

if [[ "${healthy}" != true ]]; then
  show_service_logs
  rollback
  fail "The service did not become healthy at ${HEALTHCHECK_URL}"
fi

systemctl --user --no-pager --full status "${SERVICE_NAME}"
printf 'Deployment completed: %s\n' "${IMAGE_REFERENCE}"
