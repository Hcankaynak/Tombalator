# Docker Setup for Tombalator

This project is containerized using Docker and Docker Compose.

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+

## Quick Start

1. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

2. **Start in detached mode:**
   ```bash
   docker-compose up -d --build
   ```

3. **View logs:**
   ```bash
   docker-compose logs -f
   ```

4. **Stop all services:**
   ```bash
   docker-compose down
   ```

## Services

### Backend Server (Port 3000)
- **Service name:** `server`
- **Container:** `tombalator-server`
- **Port:** `3000:3000`
- **Health check:** HTTP GET to `/`

### Frontend (Port 80)
- **Service name:** `frontend`
- **Container:** `tombalator-frontend`
- **Port:** `80:80`
- **Health check:** HTTP GET to `/health`
- **Proxy:** Nginx proxies `/api/*` and `/ws/*` to backend

## Access the Application

- **Frontend:** http://localhost
- **Backend API:** http://localhost/api
- **WebSocket:** ws://localhost/ws

## Development

### Rebuild after code changes:
```bash
docker-compose up --build
```

### View specific service logs:
```bash
docker-compose logs -f server
docker-compose logs -f frontend
```

### Execute commands in containers:
```bash
# Backend
docker-compose exec server sh

# Frontend
docker-compose exec frontend sh
```

## Environment Variables

### Frontend
- `VITE_API_URL`: API base URL (defaults to relative URL for nginx proxy)
- `VITE_WS_URL`: WebSocket URL (defaults to auto-detect from window.location)

### Backend
- `JAVA_OPTS`: JVM options (default: `-Xmx512m -Xms256m`)

## Network

All services are connected via the `tombalator-network` bridge network, allowing them to communicate using service names:
- Frontend → Backend: `http://server:3000`
- Backend can be accessed from frontend via nginx proxy

## Troubleshooting

### Port already in use
If port 80 or 3000 is already in use, modify `docker-compose.yml` to use different ports:
```yaml
ports:
  - "8080:80"  # Frontend
  - "3001:3000"  # Backend
```

### Rebuild from scratch
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

### Check container status
```bash
docker-compose ps
```

