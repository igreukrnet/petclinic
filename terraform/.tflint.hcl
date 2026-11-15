plugin "aws" {
  enabled = true
  version = "0.32.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"

  # AWS provider region validation
  region = "eu-north-1"
}

rule "terraform_comment_syntax" {
  enabled = true
}

rule "terraform_deprecated_index" {
  enabled = true
}

rule "terraform_deprecated_interpolation" {
  enabled = true
}

rule "terraform_empty_list_equality" {
  enabled = true
}

rule "terraform_module_pinned_source" {
  enabled = true
}

rule "terraform_naming_convention" {
  enabled = true
}

rule "terraform_required_providers" {
  enabled = true
}

rule "terraform_required_version" {
  enabled = true
}

rule "terraform_standard_module_structure" {
  enabled = false
}

rule "terraform_typed_variables" {
  enabled = true
}

rule "terraform_unused_declarations" {
  enabled = true
}

rule "terraform_unused_required_providers" {
  enabled = true
}

# AWS specific rules
rule "aws_instance_default_security_group" {
  enabled = true
}

rule "aws_instance_ebs_encryption" {
  enabled = true
}

rule "aws_instance_iam_instance_profile" {
  enabled = true
}

rule "aws_resource_missing_tags" {
  enabled = true
  tags   = ["Project", "Environment", "ManagedBy"]
}

rule "aws_s3_bucket_versioning" {
  enabled = true
}
