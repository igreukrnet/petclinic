terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # Uncomment and configure remote backend after creating S3 state bucket
  # backend "s3" {
  #   bucket         = "petclinic-qa-tfstate-<random>"
  #   key            = "reports/terraform.tfstate"
  #   region         = "eu-north-1"
  #   dynamodb_table = "petclinic-qa-tflock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

# ---------------------------------------------------------------------------------------------------------------------
# S3 Bucket for Allure Static Reports
# ---------------------------------------------------------------------------------------------------------------------
resource "aws_s3_bucket" "allure_reports" {
  bucket        = "${var.project_name}-${var.environment}-${random_string.bucket_suffix.result}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "allure_reports" {
  bucket                  = aws_s3_bucket.allure_reports.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "allure_reports" {
  bucket = aws_s3_bucket.allure_reports.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Archived reports of single runs (builds/<run number>/) are removed after 30 days.
# latest/ is outside the prefix and is never expired: it holds the current report and the trend history.
resource "aws_s3_bucket_lifecycle_configuration" "allure_reports" {
  bucket = aws_s3_bucket.allure_reports.id

  rule {
    id     = "expire-archived-builds"
    status = "Enabled"

    filter {
      prefix = "builds/"
    }

    expiration {
      days = 30
    }
  }
}

# ---------------------------------------------------------------------------------------------------------------------
# CloudFront Origin Access Control (OAC)
# ---------------------------------------------------------------------------------------------------------------------
resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "${var.project_name}-oac"
  description                       = "OAC for Allure Reports S3 bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# ---------------------------------------------------------------------------------------------------------------------
# CloudFront Distribution for Fast & Secure HTTPS Delivery
# ---------------------------------------------------------------------------------------------------------------------
resource "aws_cloudfront_distribution" "allure_reports" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "latest/index.html"
  comment             = "Allure Reports Distribution for PetClinic QA"

  origin {
    domain_name              = aws_s3_bucket.allure_reports.bucket_regional_domain_name
    origin_id                = "S3-${aws_s3_bucket.allure_reports.id}"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  default_cache_behavior {
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-${aws_s3_bucket.allure_reports.id}"

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

# ---------------------------------------------------------------------------------------------------------------------
# S3 Bucket Policy allowing CloudFront OAC Read Access
# ---------------------------------------------------------------------------------------------------------------------
resource "aws_s3_bucket_policy" "allow_cloudfront" {
  bucket = aws_s3_bucket.allure_reports.id
  policy = data.aws_iam_policy_document.s3_policy.json
}

data "aws_iam_policy_document" "s3_policy" {
  statement {
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.allure_reports.arn}/*"]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.allure_reports.arn]
    }
  }
}

# ---------------------------------------------------------------------------------------------------------------------
# IAM User & Least-Privilege Credentials for GitHub Actions CI/CD
# ---------------------------------------------------------------------------------------------------------------------
resource "aws_iam_user" "github_actions" {
  name = "github-actions-${var.project_name}"
}

resource "aws_iam_access_key" "github_actions" {
  user = aws_iam_user.github_actions.name
}

resource "aws_iam_user_policy" "github_actions_ci" {
  name = "github-actions-ci-reports"
  user = aws_iam_user.github_actions.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:DeleteObject"
        ]
        Resource = [
          aws_s3_bucket.allure_reports.arn,
          "${aws_s3_bucket.allure_reports.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation"
        ]
        Resource = aws_cloudfront_distribution.allure_reports.arn
      }
    ]
  })
}

