#!/bin/bash
# Script to set up AWS Parameter Store values for inventory management application
# Make sure to run 'aws configure' before running this script

# Default region and security settings
AWS_REGION=${AWS_REGION:-"us-east-1"}
KMS_KEY_ALIAS="alias/inventory-parameter-key"
CREATE_KMS_KEY=${CREATE_KMS_KEY:-"false"}

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if user is logged in to AWS
if ! aws sts get-caller-identity &> /dev/null; then
    echo "Error: You are not logged in to AWS. Please run 'aws configure' first."
    exit 1
fi

# Create KMS key if requested
if [ "$CREATE_KMS_KEY" = "true" ]; then
    echo "Creating a new KMS key for parameter encryption..."
    KMS_KEY_ID=$(aws kms create-key --description "Key for Inventory Management parameters" --region $AWS_REGION --query 'KeyMetadata.KeyId' --output text)
    aws kms create-alias --alias-name $KMS_KEY_ALIAS --target-key-id $KMS_KEY_ID --region $AWS_REGION
    echo "KMS key created with ID: $KMS_KEY_ID and alias: $KMS_KEY_ALIAS"
else
    # Try to get existing key
    KMS_KEY_ID=$(aws kms list-aliases --region $AWS_REGION --query "Aliases[?AliasName=='$KMS_KEY_ALIAS'].TargetKeyId" --output text)
    if [ -z "$KMS_KEY_ID" ]; then
        echo "No KMS key found with alias $KMS_KEY_ALIAS. Using default AWS managed key."
        KEY_PARAM=""
    else
        echo "Using existing KMS key with ID: $KMS_KEY_ID"
        KEY_PARAM="--key-id $KMS_KEY_ID"
    fi
fi

# Prompt for parameter values if not set (for interactive use)
read -p "Enter DB host (default: localhost): " DB_HOST_INPUT
read -p "Enter DB name (default: inventory): " DB_NAME_INPUT
read -p "Enter DB username (default: postgres): " DB_USERNAME_INPUT
read -sp "Enter DB password: " DB_PASSWORD_INPUT
echo ""
read -sp "Enter JWT secret: " JWT_SECRET_INPUT
echo ""

# Use input values or defaults
DB_HOST=${DB_HOST_INPUT:-"localhost"}
DB_NAME=${DB_NAME_INPUT:-"inventory"}
DB_USERNAME=${DB_USERNAME_INPUT:-"postgres"}
DB_PASSWORD=${DB_PASSWORD_INPUT:-$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 20 | head -n 1)}
JWT_SECRET=${JWT_SECRET_INPUT:-$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)}

# Create parameters in AWS Parameter Store
echo "Creating parameters in AWS Parameter Store..."
aws ssm put-parameter --name "/inventory/db/url" --value "jdbc:postgresql://${DB_HOST}:5432/${DB_NAME}" --type SecureString --region $AWS_REGION --overwrite $KEY_PARAM
aws ssm put-parameter --name "/inventory/db/username" --value "${DB_USERNAME}" --type SecureString --region $AWS_REGION --overwrite $KEY_PARAM
aws ssm put-parameter --name "/inventory/db/password" --value "${DB_PASSWORD}" --type SecureString --region $AWS_REGION --overwrite $KEY_PARAM
aws ssm put-parameter --name "/inventory/jwt/secret" --value "${JWT_SECRET}" --type SecureString --region $AWS_REGION --overwrite $KEY_PARAM

echo "Parameters created successfully!"
echo "To view them, run: aws ssm get-parameters-by-path --path /inventory --recursive --with-decryption --region $AWS_REGION"