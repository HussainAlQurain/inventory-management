variable "aws_region" {
  description = "The AWS region to deploy resources in"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "The name of the project"
  type        = string
  default     = "inventory-management"
}

variable "environment" {
  description = "The environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

# Database Variables
variable "db_allocated_storage" {
  description = "The amount of storage to allocate to the RDS instance in GB"
  type        = number
  default     = 20 # Free tier eligible is 20GB
}

variable "db_instance_class" {
  description = "The RDS instance class"
  type        = string
  default     = "db.t3.micro" # Free tier eligible
}

variable "db_name" {
  description = "The name of the database to create"
  type        = string
  default     = "inventory"
}

variable "db_username" {
  description = "The username for the database"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "The password for the database"
  type        = string
  sensitive   = true
}

variable "db_multi_az" {
  description = "Whether to enable multi-AZ deployment for the database"
  type        = bool
  default     = false # Set to true for production for high availability
}

# ECS Variables
variable "ecs_task_cpu" {
  description = "The amount of CPU to allocate to the ECS task"
  type        = number
  default     = 256 # 0.25 vCPU - minimum for Fargate
}

variable "ecs_task_memory" {
  description = "The amount of memory to allocate to the ECS task"
  type        = number
  default     = 512 # 0.5GB - minimum for Fargate
}

variable "ecs_service_desired_count" {
  description = "The number of tasks to run in the ECS service"
  type        = number
  default     = 1
}