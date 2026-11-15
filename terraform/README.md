# PetClinic QA Infrastructure as Code

Complete Terraform configuration for deploying PetClinic QA infrastructure on AWS.

## Overview

This Terraform configuration manages the following AWS resources:

| Resource | Purpose | Security |
|----------|---------|----------|
| **S3 Bucket** | Stores Allure test reports | Public access blocked, AES256 encryption |
| **CloudFront** | Fast global HTTPS delivery | OAC for secure S3 access |
| **IAM User** | GitHub Actions CI/CD credentials | Least-privilege permissions |
| **Origin Access Control** | Secure S3-to-CloudFront link | SignV4 signing protocol |

## Prerequisites

```bash
# Check Terraform version
terraform --version  # Must be >= 1.5.0

# Configure AWS credentials (one of these)
export AWS_ACCESS_KEY_ID=xxx
export AWS_SECRET_ACCESS_KEY=xxx
# OR
aws configure
```

## Quick Start

### 1. Plan Infrastructure

```bash
terraform init      # Initialize (one-time)
terraform plan      # See what will be created
```

### 2. Apply Infrastructure

```bash
terraform apply     # Deploy to AWS
```

### 3. Get Outputs

```bash
terraform output                           # All outputs
terraform output s3_bucket_name            # Specific value
terraform output -json                     # JSON format
```

### 4. Configure GitHub

After `terraform apply`, extract credentials:

```bash
# Get values to add to GitHub repository
terraform output aws_access_key_id
terraform output -raw aws_secret_access_key
terraform output s3_bucket_name
terraform output cloudfront_distribution_id
```

Add to **GitHub Settings → Secrets and variables → Actions**:

**Secrets:**
- `AWS_ACCESS_KEY_ID`: From `terraform output aws_access_key_id`
- `AWS_SECRET_ACCESS_KEY`: From `terraform output aws_secret_access_key`

**Variables:**
- `S3_BUCKET_NAME`: From `terraform output s3_bucket_name`
- `CLOUDFRONT_DISTRIBUTION_ID`: From `terraform output cloudfront_distribution_id`
- `AWS_REGION`: `eu-north-1` (or your chosen region)

## Configuration Files

| File | Purpose |
|------|---------|
| `main.tf` | Main infrastructure resources (S3, CloudFront, IAM) |
| `variables.tf` | Input variables with defaults |
| `outputs.tf` | Output values for integration |
| `.tflint.hcl` | Code quality and best-practice rules |
| `.terraform-docs.yml` | Documentation generation config |
| `.gitignore` | Prevent committing sensitive files |

## Workflows

Two GitHub Actions workflows manage Terraform:

### Validation Workflow (`terraform.yml`)
Runs on every push/PR that touches `terraform/`:
- ✅ Format check (`terraform fmt`)
- ✅ Validation (`terraform validate`)
- ✅ Linting (`tflint`)

Fails if:
- Code is not properly formatted
- Configuration is invalid
- Best-practice violations detected

### Deploy Workflow (`terraform-deploy.yml`)
Manual deployment with full control:
```
Workflow Dispatch
  ↓
  Choose: plan OR apply
  ↓
  Show changes (plan)
  ↓
  Apply if approved (apply)
```

## State Management

**Local State (Current Setup)**
- State stored in `.terraform/`
- Convenient for single developers
- ⚠️ Not recommended for teams

**Remote State (Recommended for Teams)**

Uncomment the backend configuration in `main.tf`:

```hcl
backend "s3" {
  bucket         = "petclinic-qa-tfstate-xxx"
  key            = "reports/terraform.tfstate"
  region         = "eu-north-1"
  dynamodb_table = "petclinic-qa-tflock"
  encrypt        = true
}
```

Then create the S3 bucket and DynamoDB table (one-time):

```bash
# Create S3 state bucket
aws s3 mb s3://petclinic-qa-tfstate-$(openssl rand -hex 4) --region eu-north-1

# Create DynamoDB lock table (optional but recommended)
aws dynamodb create-table \
  --table-name petclinic-qa-tflock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region eu-north-1
```

## Common Operations

### View Current State

```bash
terraform show                          # Full state
terraform state list                    # Resources only
terraform state show aws_s3_bucket.allure_reports
```

### Update Configuration

```bash
# Edit variables.tf or main.tf
terraform plan                          # Preview changes
terraform apply                         # Apply changes
```

### Destroy Infrastructure

⚠️ **Warning**: This deletes all resources!

```bash
terraform destroy                       # Interactive confirmation
terraform destroy -auto-approve        # Force (use with caution)
```

### Debugging

```bash
# Enable debug logging
export TF_LOG=DEBUG
terraform plan

# Disable debug logging
unset TF_LOG
```

## Best Practices

✅ **Do:**
- Always run `terraform plan` before `apply`
- Commit `terraform.lock.hcl` (not `.tfvars` with secrets)
- Use remote state for team environments
- Add tags to all resources (see `default_tags` in `main.tf`)
- Version lock providers in `terraform` block

❌ **Don't:**
- Commit `.tfvars` files with secrets
- Use `-auto-approve` in CI/CD without review
- Edit state files manually
- Share AWS access keys
- Store secrets in Terraform code

## Troubleshooting

### Error: "resource already exists"
```bash
terraform import aws_s3_bucket.allure_reports existing-bucket-name
```

### Error: "insufficient IAM permissions"
Ensure the AWS credentials have these permissions:
- `s3:*`
- `cloudfront:*`
- `iam:*`

### Error: "backend not initialized"
```bash
terraform init
```

## Support

For issues or questions:
1. Check `terraform validate` output
2. Review AWS CloudTrail logs
3. Check GitHub Actions workflow logs
4. Consult [Terraform AWS Provider Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)

## License

Same as petclinic project.
