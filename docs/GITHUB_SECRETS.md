# GitHub Secrets for AWS Deployment

Add the following secrets to your GitHub repository (Settings -> Secrets and variables -> Actions -> New repository secret):

## Required Secrets

| Secret Name | Description |
|-------------|-------------|
| `AWS_ACCESS_KEY_ID` | Your AWS IAM user access key |
| `AWS_SECRET_ACCESS_KEY` | Your AWS IAM user secret key |
| `DB_HOST` | RDS endpoint (e.g., `your-db-instance.xxxxxx.us-east-1.rds.amazonaws.com`) |
| `DB_NAME` | Database name (e.g., `inventory`) |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | A secure random string for JWT token signing |
| `S3_BUCKET` | S3 bucket name for deployment artifacts |

## How to Create AWS Access Keys

1. Log in to the AWS Management Console
2. Go to IAM (Identity and Access Management)
3. Create a new user or use an existing one
4. Add permissions:
   - `AmazonECR-FullAccess`
   - `AmazonECS-FullAccess`
   - `AmazonS3FullAccess`
   - `AmazonRDSFullAccess`
5. Go to the "Security credentials" tab
6. Create access key
7. Copy the Access key ID and Secret access key

**Important**: Never commit these credentials to your repository.