output "s3_bucket_name" {
  description = "Name of the S3 bucket storing Allure reports"
  value       = aws_s3_bucket.allure_reports.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket"
  value       = aws_s3_bucket.allure_reports.arn
}

output "cloudfront_distribution_id" {
  description = "ID of the CloudFront distribution"
  value       = aws_cloudfront_distribution.allure_reports.id
}

output "cloudfront_domain_name" {
  description = "Public URL domain of the CloudFront distribution"
  value       = "https://${aws_cloudfront_distribution.allure_reports.domain_name}"
}

output "aws_access_key_id" {
  description = "Access Key ID for GitHub Actions CI/CD"
  value       = aws_iam_access_key.github_actions.id
}

output "aws_secret_access_key" {
  description = "Secret Access Key for GitHub Actions CI/CD"
  value       = aws_iam_access_key.github_actions.secret
  sensitive   = true
}

