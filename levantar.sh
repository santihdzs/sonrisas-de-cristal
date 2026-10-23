#!/bin/sh
# levanta postgres, el backend con los datos de demostracion y el tunel https; imprime la direccion para la app
set -e
cd "$(dirname "$0")"
[ -f .env ] || cp .env.example .env
docker compose --profile tunel up -d --build
printf 'Esperando el túnel'
for i in $(seq 1 45); do
  url=$(docker compose logs tunel 2>/dev/null | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com' | tail -1)
  [ -n "$url" ] && break
  printf '.'; sleep 2
done
echo
echo "Backend local:              http://127.0.0.1:8080"
echo "Backend público (para la app): ${url:-no disponible, revisa: docker compose logs tunel}"
echo "Cuentas de demostración: educadora, familia, direccion, fundacion (contraseña: demo)"
