#!/bin/bash
# Script to set up AWS Parameter Store values for inventory management application
# Make sure to run 'aws configure' before running this script

# Replace these values with your actual values
DB_HOST="your-db-instance.xxxxxx.us-east-1.rds.amazonaws.com"
DB_NAME="inventory"
DB_USERNAME="postgres"
DB_PASSWORD="your-secure-password"
JWT_SECRET="your-secure-jwt-secret"
AWS_REGION="us-east-1"

# Create parameters in AWS Parameter Store
echo "Creating parameters in AWS Parameter Store..."
aws ssm put-parameter --name "/inventory/db/url" --value "jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}" --type SecureString --region $AWS_REGION --overwrite
aws ssm put-parameter --name "/inventory/db/username" --value "${DB_USERNAME}" --type SecureString --region $AWS_REGION --overwrite
aws ssm put-parameter --name "/inventory/db/password" --value "${DB_PASSWORD}" --type SecureString --region $AWS_REGION --overwrite
aws ssm put-parameter --name "/inventory/jwt/secret" --value "${JWT_SECRET}" --type SecureString --region $AWS_REGION --overwrite

echo "Parameters created successfully!"