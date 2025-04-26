# Deploy to Kubernetes

Remember to:
Keep your Kubernetes manifests version controlled with your application code
Use different configuration files for different environments (dev, staging, prod)
Consider using Helm charts for more complex deployments
Always backup your data before applying changes in production
Monitor your resources and adjust limits accordingly

## Valkey Kubernetes Deployment

### For local development with minikube:

#### Start minikube if not already running
minikube start

#### Apply the configurations
``` bash
kubectl apply -f src/main/resources/k8s/valkey-service.yml
kubectl apply -f src/main/resources/k8s/valkey-statefulset.yml
kubectl apply -f src/main/resources/k8s/valkey-configmap.yml
```

#### Verify the deployment
kubectl get pods
kubectl get services

For local development, you can either:

Use port-forwarding:
6379 or 7000

``` bash
kubectl port-forward service/valkey 6379:6379
```

Or update your hosts file to point to minikube IP:

# Get minikube IP

``` bash
minikube ip
```
# Add to /etc/hosts or C:\Windows\System32\drivers\etc\hosts
<minikube-ip> valkey

Check if the command below works correctly:

``` bash
sudo echo "<minikube-ip> valkey" | sudo tee -a /etc/hosts sudo echo "<minikube-ip> valkey" | sudo tee -a C:\Windows\System32\drivers\etc\hosts
```
