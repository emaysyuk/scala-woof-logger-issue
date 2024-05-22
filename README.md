# Run
sbt run

# Run in k8s
sbt docker:publishLocal
kubectl apply -f k8s.yaml