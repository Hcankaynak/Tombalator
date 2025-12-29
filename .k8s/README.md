# Kubernetes Deployment for Tombalator

This directory contains Kubernetes manifests for deploying the Tombalator application with HTTPS and WSS (WebSocket Secure) support.

## Prerequisites

1. **Kubernetes cluster** (v1.24+)
2. **NGINX Ingress Controller** installed
3. **cert-manager** (optional, for automatic TLS certificate management)
4. **Docker images** built and pushed to a container registry

## Files Overview

- `namespace.yaml` - Creates the `tombalator` namespace
- `backend-deployment.yaml` - Deploys the Kotlin/Ktor backend server
- `backend-service.yaml` - ClusterIP service for backend
- `frontend-deployment.yaml` - Deploys the React frontend with Nginx
- `frontend-service.yaml` - ClusterIP service for frontend
- `ingress.yaml` - Ingress with TLS and WebSocket support
- `configmap.yaml` - Configuration values
- `secret-template.yaml` - Template for TLS certificates (if not using cert-manager)
- `cert-manager-issuer.yaml` - Let's Encrypt ClusterIssuer (optional)
- `kustomization.yaml` - Kustomize configuration for easier management
- `nginx-k8s.conf` - Nginx configuration for Kubernetes (uses service names)

## HTTPS/WSS Configuration

The Ingress is configured to:
- **Terminate TLS** at the ingress level
- **Support WebSocket upgrades** (WSS) with proper headers
- **Force HTTPS redirect** for all HTTP traffic
- **Proxy WebSocket connections** with extended timeouts (24 hours)

### WebSocket Support

The Ingress includes annotations for WebSocket support:
- `nginx.ingress.kubernetes.io/proxy-read-timeout: "86400"` - 24 hour timeout
- `nginx.ingress.kubernetes.io/proxy-send-timeout: "86400"` - 24 hour timeout
- `nginx.ingress.kubernetes.io/websocket-services: "tombalator-frontend"` - Enable WebSocket
- Custom configuration snippet for upgrade headers

## Deployment Steps

### 1. Build and Push Docker Images

```bash
# Build backend
cd server
docker build -t your-registry.io/tombalator-server:latest .
docker push your-registry.io/tombalator-server:latest

# Build frontend
cd ../tombalator-ui
docker build -t your-registry.io/tombalator-frontend:latest .
docker push your-registry.io/tombalator-frontend:latest
```

### 2. Update Configuration

1. **Update `ingress.yaml`**:
   - Replace `tombalator.example.com` with your actual domain
   - Update `secretName` if using a different TLS secret name

2. **Update `configmap.yaml`**:
   - Set `ADMIN_API_KEY` to your actual admin API key
   - Adjust `VITE_API_URL` and `VITE_WS_URL` if needed (empty for production)

3. **Update `kustomization.yaml`**:
   - Replace `your-registry.io` with your container registry
   - Update image tags if needed

### 3. Deploy with kubectl

```bash
# Apply all resources
kubectl apply -k .

# Or apply individually
kubectl apply -f namespace.yaml
kubectl apply -f backend-deployment.yaml
kubectl apply -f backend-service.yaml
kubectl apply -f frontend-deployment.yaml
kubectl apply -f frontend-service.yaml
kubectl apply -f configmap.yaml
kubectl apply -f ingress.yaml
```

### 4. Set Up TLS Certificates

#### Option A: Using cert-manager (Recommended)

1. Install cert-manager:
```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml
```

2. Update `cert-manager-issuer.yaml` with your email
3. Apply the issuer:
```bash
kubectl apply -f cert-manager-issuer.yaml
```

4. The Ingress will automatically request a certificate from Let's Encrypt

#### Option B: Manual TLS Secret

1. Create a TLS secret manually:
```bash
kubectl create secret tls tombalator-tls \
  --cert=/path/to/cert.pem \
  --key=/path/to/key.pem \
  --namespace=tombalator
```

### 5. Update Frontend Nginx Configuration

If deploying to Kubernetes, you need to update the frontend Dockerfile to use `nginx-k8s.conf`:

```dockerfile
# In tombalator-ui/Dockerfile, change:
COPY nginx.conf /etc/nginx/conf.d/default.conf
# To:
COPY nginx-k8s.conf /etc/nginx/conf.d/default.conf
```

Or create a separate Dockerfile for Kubernetes that uses the Kubernetes-specific nginx config.

## Verification

### Check Pods

```bash
kubectl get pods -n tombalator
```

### Check Services

```bash
kubectl get svc -n tombalator
```

### Check Ingress

```bash
kubectl get ingress -n tombalator
kubectl describe ingress tombalator-ingress -n tombalator
```

### Check Logs

```bash
# Backend logs
kubectl logs -f deployment/tombalator-server -n tombalator

# Frontend logs
kubectl logs -f deployment/tombalator-frontend -n tombalator
```

### Test WebSocket Connection

```bash
# Test WebSocket connection (replace with your domain)
wscat -c wss://tombalator.example.com/ws/game/1234
```

## Troubleshooting

### WebSocket Connection Issues

1. Check Ingress annotations are correct
2. Verify Nginx Ingress Controller supports WebSockets
3. Check backend service is accessible from frontend pods:
   ```bash
   kubectl run -it --rm debug --image=busybox --restart=Never -n tombalator -- wget -O- http://tombalator-server:3000/
   ```

### TLS Certificate Issues

1. Check cert-manager logs:
   ```bash
   kubectl logs -n cert-manager deployment/cert-manager
   ```

2. Check certificate status:
   ```bash
   kubectl get certificate -n tombalator
   kubectl describe certificate -n tombalator
   ```

### DNS Issues

Ensure your domain points to the Ingress Controller's external IP:
```bash
kubectl get ingress -n tombalator
# Note the ADDRESS and configure your DNS A record
```

## Scaling

To scale the application:

```bash
# Scale backend
kubectl scale deployment tombalator-server --replicas=3 -n tombalator

# Scale frontend
kubectl scale deployment tombalator-frontend --replicas=3 -n tombalator
```

## Environment Variables

The `configmap.yaml` contains environment variables. To update:

```bash
kubectl edit configmap tombalator-config -n tombalator
# Then restart pods to pick up changes
kubectl rollout restart deployment/tombalator-server -n tombalator
kubectl rollout restart deployment/tombalator-frontend -n tombalator
```

## Security Notes

1. **Admin API Key**: Store in a Kubernetes Secret instead of ConfigMap for production
2. **TLS**: Always use TLS in production
3. **Resource Limits**: Adjust resource requests/limits based on your cluster capacity
4. **Network Policies**: Consider adding NetworkPolicies to restrict traffic

## Production Recommendations

1. Use a proper container registry (not `latest` tags)
2. Implement proper secret management (e.g., Sealed Secrets, External Secrets)
3. Set up monitoring and alerting
4. Configure resource quotas and limits
5. Use HorizontalPodAutoscaler for auto-scaling
6. Implement proper backup strategies
7. Use PersistentVolumes if you need persistent storage

