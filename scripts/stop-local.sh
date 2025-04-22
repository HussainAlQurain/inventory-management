#!/bin/bash
# Script to stop and clean up Docker containers for local development/testing

APP_NAME="inventory-management"
NETWORK_NAME="${APP_NAME}-network"
REMOVE_VOLUMES=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --remove-volumes)
      REMOVE_VOLUMES=true
      shift
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Stop and clean up Docker containers for local development."
      echo ""
      echo "Options:"
      echo "  --remove-volumes    Remove persistent database volumes (WARNING: destroys data)"
      echo "  --help              Display this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information."
      exit 1
      ;;
  esac
done

# Stop and remove application container
if docker ps -a | grep -q "${APP_NAME}$"; then
  echo "Stopping and removing ${APP_NAME} container..."
  docker stop ${APP_NAME} >/dev/null 2>&1
  docker rm ${APP_NAME} >/dev/null 2>&1
fi

# Stop and remove PostgreSQL container
if docker ps -a | grep -q "${APP_NAME}-postgres"; then
  echo "Stopping and removing ${APP_NAME}-postgres container..."
  docker stop ${APP_NAME}-postgres >/dev/null 2>&1
  docker rm ${APP_NAME}-postgres >/dev/null 2>&1
fi

# Remove volumes if requested
if [ "$REMOVE_VOLUMES" = true ]; then
  echo "Removing database volumes..."
  docker volume rm ${APP_NAME}-pgdata >/dev/null 2>&1 || true
fi

# Remove network if it exists and no containers are using it
CONTAINERS_USING_NETWORK=$(docker network inspect -f '{{len .Containers}}' ${NETWORK_NAME} 2>/dev/null || echo "0")
if [ "$CONTAINERS_USING_NETWORK" = "0" ]; then
  echo "Removing Docker network: ${NETWORK_NAME}..."
  docker network rm ${NETWORK_NAME} >/dev/null 2>&1 || true
fi

echo "Local environment stopped successfully."
if [ "$REMOVE_VOLUMES" = false ]; then
  echo "Database volumes were preserved. To remove them, run this script with --remove-volumes"
else
  echo "Database volumes were removed."
fi