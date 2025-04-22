#!/bin/bash
# Script to build and run the application using Docker for local development/testing

# Ensure scripts directory exists
mkdir -p $(dirname "$0")

# Default values
APP_NAME="inventory-management"
DB_NAME="inventory"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_PORT="5432"
APP_PORT="8080"
NETWORK_NAME="${APP_NAME}-network"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    --db-password)
      DB_PASSWORD="$2"
      shift 2
      ;;
    --db-port)
      DB_PORT="$2"
      shift 2
      ;;
    --app-port)
      APP_PORT="$2"
      shift 2
      ;;
    --help)
      echo "Usage: $0 [OPTIONS]"
      echo "Build and run the inventory management application using Docker."
      echo ""
      echo "Options:"
      echo "  --db-password PASSWORD   Set the database password (default: postgres)"
      echo "  --db-port PORT           Set the database port (default: 5432)"
      echo "  --app-port PORT          Set the application port (default: 8080)"
      echo "  --help                   Display this help message"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      echo "Use --help for usage information."
      exit 1
      ;;
  esac
done

# Create Docker network if it doesn't exist
if ! docker network inspect $NETWORK_NAME >/dev/null 2>&1; then
  echo "Creating Docker network: $NETWORK_NAME"
  docker network create $NETWORK_NAME
fi

# Start PostgreSQL container
echo "Starting PostgreSQL database container..."
docker run -d \
  --name ${APP_NAME}-postgres \
  --network $NETWORK_NAME \
  -e POSTGRES_DB=$DB_NAME \
  -e POSTGRES_USER=$DB_USER \
  -e POSTGRES_PASSWORD=$DB_PASSWORD \
  -p ${DB_PORT}:5432 \
  -v ${APP_NAME}-pgdata:/var/lib/postgresql/data \
  postgres:14-alpine

# Wait for PostgreSQL to start
echo "Waiting for PostgreSQL to start..."
sleep 5

# Build application Docker image
echo "Building application Docker image..."
docker build -t ${APP_NAME}:latest .

# Run the application
echo "Starting application container..."
docker run -d \
  --name ${APP_NAME} \
  --network $NETWORK_NAME \
  -p ${APP_PORT}:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://${APP_NAME}-postgres:5432/${DB_NAME} \
  -e SPRING_DATASOURCE_USERNAME=$DB_USER \
  -e SPRING_DATASOURCE_PASSWORD=$DB_PASSWORD \
  -e SPRING_PROFILES_ACTIVE=dev \
  ${APP_NAME}:latest

echo ""
echo "Application is starting up. It will be available at http://localhost:${APP_PORT}"
echo "To view logs: docker logs -f ${APP_NAME}"
echo "To stop the application: docker-compose down or run ./scripts/stop-local.sh"