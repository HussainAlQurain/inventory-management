# AWS Deployment Guide for Inventory Management System

This guide provides step-by-step instructions for deploying the Inventory Management System to AWS using ECS, ECR, and Parameter Store.

## Prerequisites

- AWS CLI installed and configured
- Terraform CLI installed
- Docker installed
- Access to the GitHub repository
- Appropriate AWS IAM permissions

## Initial Setup

### 1. Configure GitHub Repository Secrets

Add the following secrets to your GitHub repository:

- `AWS_ACCESS_KEY_ID`: Your AWS access key
- `AWS_SECRET_ACCESS_KEY`: Your AWS secret access key
- `AWS_ACCOUNT_ID`: Your AWS account ID

These secrets are used in the GitHub Actions workflows for authentication.

### 2. Create AWS Parameter Store Values

Run the setup-parameters.sh script to create required parameters:

```bash
cd terraform
chmod +x setup-parameters.sh
./setup-parameters.sh
```

This script will prompt for database and JWT configuration and store these securely in AWS Parameter Store.

### 3. Create IAM Roles

Deploy the IAM roles needed for ECS tasks:

```bash
aws cloudformation deploy \
  --template-file .aws/iam-roles.yml \
  --stack-name inventory-management-iam-roles \
  --capabilities CAPABILITY_NAMED_IAM
```

Make note of the ARNs in the output for use in Terraform variables.

### 4. Update Task Definition

Update the task definition file with your AWS account ID:

```bash
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
sed -i "s/\${AWS_ACCOUNT_ID}/$AWS_ACCOUNT_ID/g" .aws/task-definition.json
```

## Terraform Deployment

### 1. Create a terraform.tfvars File

Create a file called `terraform.tfvars` in the terraform directory:

```hcl
aws_region       = "us-east-1" # Change to your preferred region
environment      = "production"
db_instance_type = "db.t3.micro"
db_password      = "supersecurepassword" # Change this
app_name         = "inventory-management"
vpc_cidr         = "10.0.0.0/16"
```

### 2. Initialize and Apply Terraform

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

Terraform will create:
- VPC, subnets, and other networking components
- ECS cluster, service, and task definition
- RDS PostgreSQL database
- Application Load Balancer
- Auto-scaling configuration

### 3. Post-Deployment Steps

After the infrastructure is deployed:

1. Note the load balancer URL from Terraform outputs
2. Test the application health endpoint: `<load-balancer-url>/actuator/health`
3. Monitor the CloudWatch logs for any issues

## GitHub Actions CI/CD

The CI/CD pipeline is configured through GitHub Actions workflows:

1. Pushing to the `main` branch will trigger automatic deployment
2. The workflow will:
   - Build and test the Java application
   - Build and push a Docker image to ECR
   - Update the ECS service with the new image

You can view the workflow progress in the GitHub Actions tab of your repository.

## Troubleshooting

### Common Issues

1. **Container fails to start**
   
   Check CloudWatch logs for error messages:
   ```bash
   aws logs get-log-events --log-group-name /ecs/inventory-management --log-stream-name <log-stream>
   ```

2. **Database Connection Issues**
   
   Verify security group settings and parameter values:
   ```bash
   aws ssm get-parameter --name /inventory/db/url --with-decryption
   ```

3. **Deployment Timeouts**
   
   Check ECS service events:
   ```bash
   aws ecs describe-services --cluster inventory-cluster --services inventory-service
   ```

## Cleanup

To clean up all resources when no longer needed:

```bash
cd terraform
terraform destroy
```

Also delete the CloudFormation stack:

```bash
aws cloudformation delete-stack --stack-name inventory-management-iam-roles
```