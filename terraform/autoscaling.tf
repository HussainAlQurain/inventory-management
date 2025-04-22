################################################################################
# Auto Scaling Configuration
################################################################################

# Application Auto Scaling Target
resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 5
  min_capacity       = 1
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

# Scale up policy based on CPU utilization
resource "aws_appautoscaling_policy" "ecs_cpu_policy" {
  name               = "${var.project_name}-scale-cpu"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0  # Scale when CPU utilization reaches 70%
    scale_in_cooldown  = 300   # Wait 300 seconds before scaling in
    scale_out_cooldown = 60    # Wait 60 seconds before scaling out
  }
}

# Scale up policy based on memory utilization
resource "aws_appautoscaling_policy" "ecs_memory_policy" {
  name               = "${var.project_name}-scale-memory"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value       = 75.0  # Scale when memory utilization reaches 75%
    scale_in_cooldown  = 300   # Wait 300 seconds before scaling in
    scale_out_cooldown = 60    # Wait 60 seconds before scaling out
  }
}

# Scale up policy based on request count per target (ALB)
resource "aws_appautoscaling_policy" "ecs_alb_request_policy" {
  name               = "${var.project_name}-scale-alb-requests"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ALBRequestCountPerTarget"
      resource_label         = "${aws_lb.app.arn_suffix}/${aws_lb_target_group.app.arn_suffix}"
    }
    target_value       = 1000  # Scale when reaching 1000 requests per target
    scale_in_cooldown  = 300   # Wait 300 seconds before scaling in
    scale_out_cooldown = 60    # Wait 60 seconds before scaling out
  }
}