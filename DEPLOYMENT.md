# Inventory Management System - AWS Deployment Guide

This document outlines the AWS deployment process for the Inventory Management System using AWS ECS, RDS, and Terraform.

## Architecture Overview

Our deployment uses a modern containerized approach with:

- **CI/CD Pipeline**: GitHub Actions for automated building, testing, and deployment
- **Container Registry**: Amazon ECR for Docker image storage
- **Compute**: Amazon ECS with Fargate (serverless containers)
- **Database**: Amazon RDS PostgreSQL (managed database service)
- **Load Balancing**: Application Load Balancer for traffic distribution
- **Auto Scaling**: Dynamic scaling based on CPU, memory, and request count
- **Infrastructure as Code**: Terraform for repeatable infrastructure provisioning
- **Security**: Private subnets, security groups, and AWS Parameter Store for secrets

## Prerequisites

1. AWS Account with appropriate permissions
2. AWS CLI installed and configured
3. Terraform installed (v1.0.0+)
4. GitHub repository with your application code
5. Docker installed (for local testing)

## Initial Setup

### 1. Configure GitHub Secrets

Add the following secrets to your GitHub repository:

- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret key
- `DB_PASSWORD`: Database password for RDS instance

### 2. Prepare Your AWS Environment

1. Create an S3 bucket for Terraform state (optional but recommended)

```bash
aws s3 mb s3://inventory-management-tfstate
```

2. Create a terraform.tfvars file (do not commit this to version control):

```hcl
aws_region = "us-east-1"
environment = "dev"
db_password = "your-secure-password"
```

### 3. Initialize Terraform

```bash
cd terraform
terraform init
```

### 4. Deploy Infrastructure

```bash
terraform apply
```

Take note of the outputs, especially the ECR repository URL.

### 5. Initial Image Push

Build and push your initial Docker image:

```bash
# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <your-account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build the Docker image
docker build -t inventory-management .

# Tag the image
docker tag inventory-management:latest <your-ecr-repo-url>:latest

# Push the image
docker push <your-ecr-repo-url>:latest
```

## CI/CD Workflow

The GitHub Actions workflow (.github/workflows/aws-deploy.yml) will:

1. Build and test the application when changes are pushed
2. Create a Docker image and push it to ECR
3. Update the ECS service with the new image
4. Deploy the changes to AWS

## Scaling Configuration

The application will automatically scale based on:

- CPU utilization exceeding 70%
- Memory utilization exceeding 75%
- Request count exceeding 1000 per target

## Cost Management

Our setup uses AWS Free Tier components where possible:

- **RDS**: db.t3.micro instance (free tier eligible)
- **ECS Fargate**: Minimal configuration with 256 CPU units and 512 MB memory
- **Auto Scaling**: Scale down to 1 instance during low traffic

## Monitoring and Logging

- **CloudWatch**: Container logs are sent to CloudWatch
- **Health Checks**: Application health is monitored via the /actuator/health endpoint

## Database Migration

The database will be initialized with the schema defined in your application. For subsequent migrations, use your application's migration mechanism.

## Common Issues and Troubleshooting

### Container Fails to Start

Check the CloudWatch logs:

```bash
aws logs get-log-events --log-group-name /ecs/inventory-management --log-stream-name <log-stream-name>
```

### Database Connection Issues

Verify the database security group allows traffic from the application security group, and check the environment variables for correct database configuration.

### Slow Application Performance

Check the metrics in CloudWatch and consider adjusting the scaling thresholds or increasing the base capacity.

## Security Best Practices

1. Rotate AWS credentials regularly
2. Use IAM roles with least privilege
3. Regularly update dependencies
4. Enable database encryption
5. Consider enabling AWS WAF for the load balancer

## Future Enhancements

- Add HTTPS support with AWS Certificate Manager
- Implement Blue/Green deployments
- Set up cross-region replication for disaster recovery
- Implement AWS X-Ray for distributed tracing