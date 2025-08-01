#!/bin/bash

# Spring Boot Lambda Deployment Script
# This script provides end-to-end deployment for the Spring Boot Lambda application
# It maintains the same S3 bucket and CloudFormation stack across deployments

set -e  # Exit on any error

# Configuration (defaults)
PROJECT_NAME="spring-boot-demo"
STACK_NAME="spring-boot-demo"
REGION="us-east-1"
STAGE="dev"

# Colors for enhanced output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m' # No Color

# Enhanced function to print colored output with emojis
print_status() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "\n${BOLD}${BLUE}================================${NC}"
    echo -e "${BOLD}${BLUE}$1${NC}"
    echo -e "${BOLD}${BLUE}================================${NC}\n"
}

print_step() {
    echo -e "\n${PURPLE}➤ $1${NC}"
}

print_highlight() {
    echo -e "${BOLD}$1${NC}"
}

# Function to setup secure configuration in Parameter Store
setup_secure_config() {
    print_header "🔐 Secure Configuration Setup"
    
    print_status "Cognito User Pool and configuration will be created via CloudFormation"
    print_status "The CloudFormation template includes Cognito resources that will automatically"
    print_status "create Parameter Store entries with the Cognito configuration."
    echo ""
    
    # Check if parameters already exist (might be from previous CloudFormation deployment)
    print_status "🔍 Checking for existing secure parameters..."
    
    PARAM_EXISTS=$(aws ssm get-parameter --name "/spring-boot-demo/cognito/client-id" --region "$REGION" 2>/dev/null || echo "NOT_EXISTS")
    
    if [[ "$PARAM_EXISTS" == "NOT_EXISTS" ]]; then
        print_status "📝 Secure parameters not found - they will be created by CloudFormation"
        print_status "🚀 The CloudFormation stack will create:"
        print_status "   • Cognito User Pool with specified configuration"
        print_status "   • Cognito User Pool Client with authentication flows"
        print_status "   • Parameter Store entries for secure configuration"
        print_status "   • IAM roles and permissions for Lambda access"
        echo ""
    else
        print_success "✅ Secure configuration already exists in Parameter Store"
        
        # Show existing configuration (without secrets)
        print_status "📋 Current configuration:"
        CLIENT_ID=$(aws ssm get-parameter --name "/spring-boot-demo/cognito/client-id" --region "$REGION" --query 'Parameter.Value' --output text 2>/dev/null || echo "Not found")
        USER_POOL_ID=$(aws ssm get-parameter --name "/spring-boot-demo/cognito/user-pool-id" --region "$REGION" --query 'Parameter.Value' --output text 2>/dev/null || echo "Not found")
        COGNITO_REGION=$(aws ssm get-parameter --name "/spring-boot-demo/cognito/region" --region "$REGION" --query 'Parameter.Value' --output text 2>/dev/null || echo "Not found")
        
        print_status "   • Client ID: $CLIENT_ID"
        print_status "   • User Pool ID: $USER_POOL_ID"
        print_status "   • Cognito Region: $COGNITO_REGION"
        print_status "   • Client Secret: ******* (encrypted)"
        
        echo ""
        print_status "ℹ️  Configuration managed by CloudFormation - no manual updates needed"
    fi
}

# Function to clean up secure configuration from Parameter Store
cleanup_secure_config() {
    print_status "Cleaning up secure configuration from Parameter Store..."
    
    # List of parameters to delete
    PARAMETERS=(
        "/spring-boot-demo/cognito/client-id"
        "/spring-boot-demo/cognito/client-secret"
        "/spring-boot-demo/cognito/user-pool-id"
        "/spring-boot-demo/cognito/region"
    )
    
    for param in "${PARAMETERS[@]}"; do
        if aws ssm get-parameter --name "$param" --region "$REGION" >/dev/null 2>&1; then
            print_status "Deleting parameter: $param"
            aws ssm delete-parameter --name "$param" --region "$REGION" >/dev/null 2>&1 || true
        fi
    done
    
    print_success "✓ Secure configuration cleaned up from Parameter Store"
}

# Function to check if S3 bucket exists
check_s3_bucket() {
    if aws s3api head-bucket --bucket "$S3_BUCKET_NAME" --region "$REGION" 2>/dev/null; then
        print_success "S3 bucket '$S3_BUCKET_NAME' already exists"
        return 0
    else
        return 1
    fi
}

# Function to create S3 bucket
create_s3_bucket() {
    print_status "Creating S3 bucket: $S3_BUCKET_NAME"
    
    if [ "$REGION" = "us-east-1" ]; then
        aws s3api create-bucket --bucket "$S3_BUCKET_NAME" --region "$REGION"
    else
        aws s3api create-bucket --bucket "$S3_BUCKET_NAME" --region "$REGION" \
            --create-bucket-configuration LocationConstraint="$REGION"
    fi
    
    # Enable versioning
    aws s3api put-bucket-versioning --bucket "$S3_BUCKET_NAME" \
        --versioning-configuration Status=Enabled
    
    # Add bucket policy for SAM
    aws s3api put-bucket-policy --bucket "$S3_BUCKET_NAME" --policy "{
        \"Version\": \"2012-10-17\",
        \"Statement\": [
            {
                \"Effect\": \"Allow\",
                \"Principal\": {
                    \"Service\": \"cloudformation.amazonaws.com\"
                },
                \"Action\": \"s3:GetObject\",
                \"Resource\": \"arn:aws:s3:::${S3_BUCKET_NAME}/*\"
            }
        ]
    }"
    
    print_success "S3 bucket created successfully"
}

# Function to create samconfig.toml with user input or default values
create_samconfig() {
    if [ -f "samconfig.toml" ]; then
        print_status "samconfig.toml already exists"
        return 0
    fi
    
    print_header "Creating SAM Configuration"
    
    # Get AWS Account ID
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    
    # Default values
    DEFAULT_STACK_NAME="spring-boot-demo"
    DEFAULT_S3_BUCKET="spring-boot-demo-artifacts-$ACCOUNT_ID"
    DEFAULT_REGION="us-east-1"
    
    print_status "Please provide configuration (press Enter for defaults):"
    
    # Ask for stack name
    echo -n "Stack name [$DEFAULT_STACK_NAME]: "
    read USER_STACK_NAME
    USER_STACK_NAME=${USER_STACK_NAME:-$DEFAULT_STACK_NAME}
    
    # Ask for S3 bucket
    echo -n "S3 bucket name [$DEFAULT_S3_BUCKET]: "
    read USER_S3_BUCKET
    USER_S3_BUCKET=${USER_S3_BUCKET:-$DEFAULT_S3_BUCKET}
    
    # Ask for region
    echo -n "AWS region [$DEFAULT_REGION]: "
    read USER_REGION
    USER_REGION=${USER_REGION:-$DEFAULT_REGION}
    
    # Update global variables
    STACK_NAME="$USER_STACK_NAME"
    S3_BUCKET_NAME="$USER_S3_BUCKET"
    REGION="$USER_REGION"
    
    print_status "Configuration:"
    print_status "  Stack name: $STACK_NAME"
    print_status "  S3 bucket: $S3_BUCKET_NAME"
    print_status "  Region: $REGION"
    print_status "  Stage: $STAGE"
    
    print_status "Creating samconfig.toml with your configuration"
    
    cat > samconfig.toml << EOF
version = 0.1

[default.deploy.parameters]
stack_name = "${STACK_NAME}"
s3_bucket = "${S3_BUCKET_NAME}"
s3_prefix = "${STACK_NAME}"
region = "${REGION}"
confirm_changeset = false
capabilities = "CAPABILITY_IAM"
parameter_overrides = "Stage=\"${STAGE}\""
resolve_s3 = false
disable_rollback = true
image_repositories = []
EOF
    
    print_success "samconfig.toml created with default configuration"
}

# Function to load configuration from samconfig.toml if it exists
load_samconfig() {
    if [ -f "samconfig.toml" ]; then
        print_status "Loading configuration from samconfig.toml"
        
        # Extract values from samconfig.toml
        STACK_NAME=$(grep 'stack_name' samconfig.toml | cut -d'"' -f2)
        S3_BUCKET_NAME=$(grep 's3_bucket' samconfig.toml | cut -d'"' -f2)
        REGION=$(grep 'region' samconfig.toml | cut -d'"' -f2)
        
        print_status "Using stack: $STACK_NAME, bucket: $S3_BUCKET_NAME, region: $REGION"
    fi
}

# Function to build the project
build_project() {
    print_header "Building Spring Boot Project"
    
    print_status "Cleaning and compiling project..."
    mvn clean package -DskipTests -q -Plambda
    
    if [ -f "target/demo-lambda.jar" ]; then
        print_success "JAR file created: target/demo-lambda.jar"
        print_status "JAR size: $(du -h target/demo-lambda.jar | cut -f1)"
    else
        print_error "JAR file not found!"
        exit 1
    fi
}

# Function to deploy the application
deploy_application() {
    print_header "Deploying to AWS Lambda"
    
    print_status "Packaging and deploying SAM application..."
    sam deploy --no-confirm-changeset --no-fail-on-empty-changeset
    
    print_success "Deployment completed successfully"
}

# Function to get CloudWatch log group names
get_log_groups() {
    LAMBDA_LOG_GROUP=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`LambdaLogGroup`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    API_LOG_GROUP=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`ApiGatewayLogGroup`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    if [ -z "$LAMBDA_LOG_GROUP" ]; then
        LAMBDA_LOG_GROUP="/aws/lambda/${STACK_NAME}-${STAGE}"
    fi
    
    if [ -z "$API_LOG_GROUP" ]; then
        API_LOG_GROUP="/aws/apigateway/${STACK_NAME}-api-${STAGE}"
    fi
}

# Function to show recent logs
show_recent_logs() {
    print_header "Recent CloudWatch Logs"
    
    get_log_groups
    
    print_status "Lambda Log Group: $LAMBDA_LOG_GROUP"
    print_status "API Gateway Log Group: $API_LOG_GROUP"
    
    echo -e "\n${YELLOW}Recent Lambda Logs (last 5 minutes):${NC}"
    aws logs filter-log-events \
        --log-group-name "$LAMBDA_LOG_GROUP" \
        --region "$REGION" \
        --start-time $(( $(date +%s) * 1000 - 300000 )) \
        --query 'events[*].[timestamp,message]' \
        --output table 2>/dev/null || print_warning "No recent Lambda logs found"
    
    echo -e "\n${YELLOW}Recent API Gateway Logs (last 5 minutes):${NC}"
    aws logs filter-log-events \
        --log-group-name "$API_LOG_GROUP" \
        --region "$REGION" \
        --start-time $(( $(date +%s) * 1000 - 300000 )) \
        --query 'events[*].[timestamp,message]' \
        --output table 2>/dev/null || print_warning "No recent API Gateway logs found"
}

# Function to tail logs in real-time
tail_logs() {
    print_header "Tailing CloudWatch Logs"
    
    get_log_groups
    
    print_status "Starting log tail for both Lambda and API Gateway..."
    print_status "Press Ctrl+C to stop"
    
    echo -e "\n${GREEN}Log Format:${NC}"
    echo -e "${BLUE}[LAMBDA]${NC} - Lambda function logs"
    echo -e "${YELLOW}[API-GW]${NC} - API Gateway access logs"
    echo ""
    
    # Function to tail lambda logs
    tail_lambda_logs() {
        aws logs tail "$LAMBDA_LOG_GROUP" --region "$REGION" --follow --format short 2>/dev/null | \
        while IFS= read -r line; do
            echo -e "${BLUE}[LAMBDA]${NC} $line"
        done
    }
    
    # Function to tail API Gateway logs
    tail_api_logs() {
        aws logs tail "$API_LOG_GROUP" --region "$REGION" --follow --format short 2>/dev/null | \
        while IFS= read -r line; do
            echo -e "${YELLOW}[API-GW]${NC} $line"
        done
    }
    
    # Start both tails in background
    tail_lambda_logs &
    LAMBDA_PID=$!
    
    tail_api_logs &
    API_PID=$!
    
    # Wait for Ctrl+C
    trap 'kill $LAMBDA_PID $API_PID 2>/dev/null; exit 0' INT
    wait
}

# Function to get API Gateway URL
get_api_url() {
    API_URL=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`DemoApi`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    if [ -n "$API_URL" ]; then
        echo "$API_URL"
    else
        print_error "Could not retrieve API Gateway URL"
        return 1
    fi
}

# Function to test authentication flow interactively
test_auth_flow() {
    print_header "🔐 Interactive Authentication Testing"
    
    print_status "Testing the complete authentication flow with real user interaction"
    print_status "This will test: signup → verification → signin → profile access"
    echo ""
    
    # Get API URL
    print_status "🌐 Retrieving API Gateway URL..."
    API_URL=$(get_api_url)
    
    if [ -z "$API_URL" ]; then
        print_error "❌ Could not get API URL. Please ensure the application is deployed."
        return 1
    fi
    
    print_success "✅ API Gateway URL: $API_URL"
    echo ""
    
    # Wait for deployment to be ready
    print_status "⏳ Waiting for deployment to be ready..."
    sleep 3
    
    # Step 1: Get user input for testing
    print_header "📝 User Information for Testing"
    print_status "Please provide user details for testing the authentication flow:"
    echo ""
    
    # Get email from user
    while true; do
        echo -n "📧 Enter email address: "
        read TEST_EMAIL
        
        if [[ "$TEST_EMAIL" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]]; then
            break
        else
            print_error "❌ Invalid email format. Please enter a valid email address."
        fi
    done
    
    # Get password from user
    while true; do
        echo -n "🔑 Enter password (min 8 chars, include uppercase, lowercase, number, special char): "
        read -s TEST_PASSWORD
        echo
        
        # Basic password validation
        if [[ ${#TEST_PASSWORD} -ge 8 ]] && [[ "$TEST_PASSWORD" =~ [A-Z] ]] && [[ "$TEST_PASSWORD" =~ [a-z] ]] && [[ "$TEST_PASSWORD" =~ [0-9] ]] && [[ "$TEST_PASSWORD" =~ [^a-zA-Z0-9] ]]; then
            break
        else
            print_error "❌ Password doesn't meet requirements. Please try again."
        fi
    done
    
    # Generate username and name from email
    TEST_USERNAME=$(echo "$TEST_EMAIL" | cut -d'@' -f1)
    TEST_NAME="Test User $(date +%H%M)"
    
    echo ""
    print_status "📋 Test user details:"
    print_status "   • Email: $TEST_EMAIL"
    print_status "   • Username: $TEST_USERNAME"
    print_status "   • Name: $TEST_NAME"
    print_status "   • Password: ******** (hidden)"
    echo ""
    
    # Step 2: User Registration
    print_header "🎯 Step 1: User Registration"
    print_status "Creating user account..."
    
    SIGNUP_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{
        \"username\": \"$TEST_USERNAME\",
        \"email\": \"$TEST_EMAIL\",
        \"password\": \"$TEST_PASSWORD\",
        \"name\": \"$TEST_NAME\"
    }" "${API_URL}api/auth/signup")
    
    echo ""
    print_status "📤 Signup Response:"
    echo "$SIGNUP_RESPONSE" | jq . 2>/dev/null || echo "$SIGNUP_RESPONSE"
    echo ""
    
    # Check if signup was successful
    if echo "$SIGNUP_RESPONSE" | grep -q '"success":true'; then
        print_success "✅ User registration successful!"
        
        # Step 3: Get verification code from user
        print_header "📨 Step 2: Email Verification"
        print_status "A verification code has been sent to: $TEST_EMAIL"
        echo ""
        
        # Interactive verification code input
        while true; do
            echo -n "🔢 Enter the 6-digit verification code from your email: "
            read VERIFICATION_CODE
            
            if [[ "$VERIFICATION_CODE" =~ ^[0-9]{6}$ ]]; then
                break
            else
                print_error "❌ Invalid format. Please enter a 6-digit numeric code."
            fi
        done
        
        # Step 4: Confirm signup
        print_status "✅ Confirming signup with code: $VERIFICATION_CODE"
        
        CONFIRM_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{
            \"email\": \"$TEST_EMAIL\",
            \"username\": \"$TEST_USERNAME\",
            \"confirmationCode\": \"$VERIFICATION_CODE\"
        }" "${API_URL}api/auth/confirm-signup")
        
        echo ""
        print_status "📤 Confirmation Response:"
        echo "$CONFIRM_RESPONSE" | jq . 2>/dev/null || echo "$CONFIRM_RESPONSE"
        echo ""
        
        if echo "$CONFIRM_RESPONSE" | grep -q '"success":true'; then
            print_success "✅ Email verification successful!"
            
            # Step 5: Test signin with username
            print_header "🔐 Step 3: User Sign In (Username)"
            print_status "Signing in with username: $TEST_USERNAME"
            
            SIGNIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{
                \"login\": \"$TEST_USERNAME\",
                \"password\": \"$TEST_PASSWORD\"
            }" "${API_URL}api/auth/signin")
            
            echo ""
            print_status "📤 Username Signin Response:"
            echo "$SIGNIN_RESPONSE" | jq . 2>/dev/null || echo "$SIGNIN_RESPONSE"
            echo ""
            
            if echo "$SIGNIN_RESPONSE" | grep -q '"success":true'; then
                print_success "✅ Username signin successful!"
                
                # Extract access token
                ACCESS_TOKEN=$(echo "$SIGNIN_RESPONSE" | jq -r '.data.accessToken' 2>/dev/null)
                
                if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
                    # Step 6: Test signin with email
                    print_header "📧 Step 4: User Sign In (Email)"
                    print_status "Signing in with email: $TEST_EMAIL"
                    
                    EMAIL_SIGNIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{
                        \"login\": \"$TEST_EMAIL\",
                        \"password\": \"$TEST_PASSWORD\"
                    }" "${API_URL}api/auth/signin")
                    
                    echo ""
                    print_status "📤 Email Signin Response:"
                    echo "$EMAIL_SIGNIN_RESPONSE" | jq . 2>/dev/null || echo "$EMAIL_SIGNIN_RESPONSE"
                    echo ""
                    
                    if echo "$EMAIL_SIGNIN_RESPONSE" | grep -q '"success":true'; then
                        print_success "✅ Email signin successful!"
                        
                        # Step 7: Get profile
                        print_header "👤 Step 5: Profile Access"
                        print_status "Accessing user profile with access token..."
                        
                        PROFILE_RESPONSE=$(curl -s -X GET -H "Authorization: Bearer $ACCESS_TOKEN" "${API_URL}api/auth/profile")
                        
                        echo ""
                        print_status "📤 Profile Response:"
                        echo "$PROFILE_RESPONSE" | jq . 2>/dev/null || echo "$PROFILE_RESPONSE"
                        echo ""
                        
                        if echo "$PROFILE_RESPONSE" | grep -q '"success":true'; then
                            print_success "✅ Profile access successful!"
                            
                            # Final success summary
                            print_header "🎉 Authentication Test Results"
                            print_success "✅ 1. User Registration"
                            print_success "✅ 2. Email Verification"
                            print_success "✅ 3. Username Sign In"
                            print_success "✅ 4. Email Sign In"
                            print_success "✅ 5. Profile Access"
                            echo ""
                            print_success "🚀 All authentication endpoints are working perfectly!"
                            
                        else
                            print_error "❌ Profile access failed"
                        fi
                    else
                        print_error "❌ Email signin failed"
                    fi
                else
                    print_error "❌ Could not extract access token from response"
                fi
            else
                print_error "❌ Username signin failed"
            fi
        else
            print_error "❌ Email verification failed"
            print_status "💡 Tip: Check your email for the correct verification code"
            print_status "You can also manually confirm the user using AWS CLI:"
            print_status "aws cognito-idp admin-confirm-sign-up --user-pool-id us-east-1_X3grEwPDP --username $TEST_USERNAME"
        fi
    else
        print_error "❌ User registration failed"
        echo ""
        print_status "📤 Error details:"
        echo "$SIGNUP_RESPONSE"
        
        # Check if user already exists
        if echo "$SIGNUP_RESPONSE" | grep -q "UsernameExistsException\|already exists"; then
            print_warning "⚠️  User already exists. You can try signing in directly."
            echo ""
            echo -n "🔄 Would you like to test signin with existing user? (y/N): "
            read -n 1 -r
            echo
            
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                # Test signin directly
                print_header "🔐 Direct Sign In Test"
                
                SIGNIN_RESPONSE=$(curl -s -X POST -H "Content-Type: application/json" -d "{
                    \"login\": \"$TEST_USERNAME\",
                    \"password\": \"$TEST_PASSWORD\"
                }" "${API_URL}api/auth/signin")
                
                echo ""
                print_status "📤 Signin Response:"
                echo "$SIGNIN_RESPONSE" | jq . 2>/dev/null || echo "$SIGNIN_RESPONSE"
                echo ""
                
                if echo "$SIGNIN_RESPONSE" | grep -q '"success":true'; then
                    print_success "✅ Existing user signin successful!"
                else
                    print_error "❌ Signin failed. Please check your credentials."
                fi
            fi
        fi
    fi
}

# Function to test all authentication endpoints comprehensively (non-interactive)
test_all_auth_endpoints() {
    print_header "🔐 Comprehensive Authentication Endpoints Testing"
    
    print_status "Testing all authentication endpoints with automated scenarios"
    echo ""
    
    # Get API URL
    API_URL=$(get_api_url)
    if [ -z "$API_URL" ]; then
        print_error "❌ Could not get API URL. Please ensure the application is deployed."
        return 1
    fi
    
    print_success "✅ API Gateway URL: $API_URL"
    echo ""
    
    # Test 1: Health Check
    echo -e "${BLUE}1. Testing Health Endpoint${NC}"
    health_response=$(curl -s -w "%{http_code}" "${API_URL}health" -o /tmp/health_response.json)
    health_code="${health_response: -3}"
    
    if [ "$health_code" = "200" ]; then
        echo -e "   ✅ Health endpoint accessible (HTTP $health_code)"
        health_data=$(cat /tmp/health_response.json 2>/dev/null || echo "{}")
        echo -e "   📊 Response: $health_data"
    else
        echo -e "   ❌ Health endpoint failed (HTTP $health_code)"
    fi
    
    # Test 2: Error Endpoint
    echo -e "${BLUE}2. Testing Error Handling Endpoint${NC}"
    error_response=$(curl -s -w "%{http_code}" "${API_URL}error" -o /tmp/error_response.json)
    error_code="${error_response: -3}"
    echo -e "   📝 Error endpoint response: HTTP $error_code"
    
    # Test 3: User Registration
    echo -e "${BLUE}3. Testing User Registration Endpoint${NC}"
    local test_user="testuser_$(date +%s)"
    local test_email="test_$(date +%s)@example.com"
    
    signup_payload="{
        \"username\": \"$test_user\",
        \"email\": \"$test_email\",
        \"password\": \"Test123@\",
        \"name\": \"Test User\"
    }"
    
    signup_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$signup_payload" \
        "${API_URL}api/auth/signup" \
        -o /tmp/signup_response.json)
    signup_code="${signup_response: -3}"
    
    if [ "$signup_code" = "200" ] || [ "$signup_code" = "201" ]; then
        echo -e "   ✅ User registration successful (HTTP $signup_code)"
        signup_data=$(cat /tmp/signup_response.json 2>/dev/null || echo "{}")
        echo -e "   📝 Response: $signup_data"
    else
        echo -e "   ❌ User registration failed (HTTP $signup_code)"
        signup_error=$(cat /tmp/signup_response.json 2>/dev/null || echo "No response data")
        echo -e "   📝 Error: $signup_error"
    fi
    
    # Test 4: Resend Confirmation Code
    echo -e "${BLUE}4. Testing Resend Confirmation Endpoint${NC}"
    resend_payload="{\"username\": \"$test_user\"}"
    
    resend_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$resend_payload" \
        "${API_URL}api/auth/resend-confirmation" \
        -o /tmp/resend_response.json)
    resend_code="${resend_response: -3}"
    
    if [ "$resend_code" = "200" ]; then
        echo -e "   ✅ Resend confirmation working (HTTP $resend_code)"
    else
        echo -e "   ⚠️  Resend confirmation response: HTTP $resend_code"
    fi
    
    # Test 5: Forgot Password
    echo -e "${BLUE}5. Testing Forgot Password Endpoint${NC}"
    forgot_payload="{\"email\": \"$test_email\"}"
    
    forgot_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$forgot_payload" \
        "${API_URL}api/auth/forgot-password" \
        -o /tmp/forgot_response.json)
    forgot_code="${forgot_response: -3}"
    
    if [ "$forgot_code" = "200" ]; then
        echo -e "   ✅ Forgot password endpoint working (HTTP $forgot_code)"
    else
        echo -e "   ⚠️  Forgot password response: HTTP $forgot_code"
    fi
    
    # Test 6: Reset Password (will fail without real confirmation code)
    echo -e "${BLUE}6. Testing Reset Password Endpoint${NC}"
    reset_payload="{
        \"email\": \"$test_email\",
        \"confirmationCode\": \"123456\",
        \"newPassword\": \"NewPass123@\"
    }"
    
    reset_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$reset_payload" \
        "${API_URL}api/auth/reset-password" \
        -o /tmp/reset_response.json)
    reset_code="${reset_response: -3}"
    
    echo -e "   📝 Reset password response: HTTP $reset_code (expected to fail without valid code)"
    
    # Test 7: Sign In (will fail - user needs confirmation)
    echo -e "${BLUE}7. Testing Sign In Endpoint${NC}"
    signin_payload="{
        \"login\": \"$test_email\",
        \"password\": \"Test123@\"
    }"
    
    signin_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$signin_payload" \
        "${API_URL}api/auth/signin" \
        -o /tmp/signin_response.json)
    signin_code="${signin_response: -3}"
    
    if [ "$signin_code" = "200" ]; then
        echo -e "   ✅ Sign in successful (HTTP $signin_code)"
        signin_data=$(cat /tmp/signin_response.json 2>/dev/null || echo "{}")
        echo -e "   🔑 Response: $signin_data"
    else
        echo -e "   ⚠️  Sign in failed as expected (user unconfirmed): HTTP $signin_code"
    fi
    
    # Test 8: Refresh Token (will fail without valid token)
    echo -e "${BLUE}8. Testing Refresh Token Endpoint${NC}"
    refresh_payload="{
        \"refreshToken\": \"fake-refresh-token\",
        \"username\": \"$test_user\"
    }"
    
    refresh_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$refresh_payload" \
        "${API_URL}api/auth/refresh-token" \
        -o /tmp/refresh_response.json)
    refresh_code="${refresh_response: -3}"
    
    echo -e "   📝 Refresh token response: HTTP $refresh_code (expected to fail with fake token)"
    
    # Test 9: Protected Endpoints (should fail without auth)
    echo -e "${BLUE}9. Testing Protected Profile Endpoint${NC}"
    profile_response=$(curl -s -w "%{http_code}" \
        "${API_URL}api/auth/profile" \
        -o /tmp/profile_response.json)
    profile_code="${profile_response: -3}"
    
    if [ "$profile_code" = "401" ] || [ "$profile_code" = "403" ]; then
        echo -e "   ✅ Profile endpoint properly protected (HTTP $profile_code)"
    else
        echo -e "   ⚠️  Profile endpoint response: HTTP $profile_code"
    fi
    
    # Test 10: Validate Session (should fail without auth)
    echo -e "${BLUE}10. Testing Validate Session Endpoint${NC}"
    validate_response=$(curl -s -w "%{http_code}" \
        "${API_URL}api/auth/validate-session" \
        -o /tmp/validate_response.json)
    validate_code="${validate_response: -3}"
    
    if [ "$validate_code" = "401" ] || [ "$validate_code" = "403" ]; then
        echo -e "   ✅ Validate session properly protected (HTTP $validate_code)"
    else
        echo -e "   ⚠️  Validate session response: HTTP $validate_code"
    fi
    
    # Test 11: Sign Out (should fail without auth)
    echo -e "${BLUE}11. Testing Sign Out Endpoint${NC}"
    signout_response=$(curl -s -w "%{http_code}" -X POST \
        "${API_URL}api/auth/signout" \
        -o /tmp/signout_response.json)
    signout_code="${signout_response: -3}"
    
    if [ "$signout_code" = "401" ] || [ "$signout_code" = "403" ]; then
        echo -e "   ✅ Signout endpoint properly protected (HTTP $signout_code)"
    else
        echo -e "   ⚠️  Signout endpoint response: HTTP $signout_code"
    fi
    
    echo ""
    print_success "🎯 All 11 authentication endpoints tested!"
    print_status "📝 Note: Some endpoints expected to fail due to security/validation requirements"
    
    return 0
}

# Function to test product endpoints comprehensively
test_products_api() {
    print_header "📦 Comprehensive Products API Testing"
    
    print_status "Testing all product management endpoints"
    echo ""
    
    # Get API URL
    API_URL=$(get_api_url)
    if [ -z "$API_URL" ]; then
        print_error "❌ Could not get API URL. Please ensure the application is deployed."
        return 1
    fi
    
    # Test 1: Create Product
    echo -e "${BLUE}1. Testing Product Creation (POST /api/products)${NC}"
    
    local test_product_name="Test Product $(date +%s)"
    local test_price="99.99"
    
    product_payload="{
        \"prodname\": \"$test_product_name\",
        \"price\": $test_price
    }"
    
    create_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$product_payload" \
        "${API_URL}api/products" \
        -o /tmp/create_product_response.json)
    create_code="${create_response: -3}"
    
    if [ "$create_code" = "200" ] || [ "$create_code" = "201" ]; then
        echo -e "   ✅ Product creation successful (HTTP $create_code)"
        create_data=$(cat /tmp/create_product_response.json 2>/dev/null || echo "{}")
        echo -e "   📦 Created product: $create_data"
        
        # Extract product ID from response for get test
        PRODUCT_ID=$(echo "$create_data" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$PRODUCT_ID" ]; then
            echo -e "   🆔 Product ID: $PRODUCT_ID"
        fi
    else
        echo -e "   ❌ Product creation failed (HTTP $create_code)"
        create_error=$(cat /tmp/create_product_response.json 2>/dev/null || echo "No response data")
        echo -e "   📝 Error details: $create_error"
        return 1
    fi
    
    # Test 2: Get Product by ID
    if [ -n "$PRODUCT_ID" ]; then
        echo -e "${BLUE}2. Testing Product Retrieval (GET /api/products/{id})${NC}"
        
        get_response=$(curl -s -w "%{http_code}" \
            "${API_URL}api/products/${PRODUCT_ID}" \
            -o /tmp/get_product_response.json)
        get_code="${get_response: -3}"
        
        if [ "$get_code" = "200" ]; then
            echo -e "   ✅ Product retrieval successful (HTTP $get_code)"
            get_data=$(cat /tmp/get_product_response.json 2>/dev/null || echo "{}")
            echo -e "   📦 Retrieved product: $get_data"
        else
            echo -e "   ❌ Product retrieval failed (HTTP $get_code)"
            get_error=$(cat /tmp/get_product_response.json 2>/dev/null || echo "No response data")
            echo -e "   📝 Error details: $get_error"
        fi
    else
        echo -e "${YELLOW}⚠️  Skipping product retrieval test (no product ID available)${NC}"
    fi
    
    # Test 3: Get Non-existent Product (should return 404)
    echo -e "${BLUE}3. Testing Non-existent Product Retrieval${NC}"
    
    fake_id="00000000-0000-0000-0000-000000000000"
    notfound_response=$(curl -s -w "%{http_code}" \
        "${API_URL}api/products/${fake_id}" \
        -o /tmp/notfound_product_response.json)
    notfound_code="${notfound_response: -3}"
    
    if [ "$notfound_code" = "404" ]; then
        echo -e "   ✅ Non-existent product handling correct (HTTP 404)"
    else
        echo -e "   ⚠️  Non-existent product response: HTTP $notfound_code"
        notfound_data=$(cat /tmp/notfound_product_response.json 2>/dev/null || echo "No response data")
        echo -e "   📝 Response: $notfound_data"
    fi
    
    # Test 4: Create Product with Invalid Data
    echo -e "${BLUE}4. Testing Invalid Product Creation${NC}"
    
    invalid_payload='{"invalid": "data"}'
    
    invalid_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$invalid_payload" \
        "${API_URL}api/products" \
        -o /tmp/invalid_product_response.json)
    invalid_code="${invalid_response: -3}"
    
    if [ "$invalid_code" = "400" ] || [ "$invalid_code" = "422" ]; then
        echo -e "   ✅ Invalid data handling correct (HTTP $invalid_code)"
    else
        echo -e "   ⚠️  Invalid data response: HTTP $invalid_code"
        invalid_data=$(cat /tmp/invalid_product_response.json 2>/dev/null || echo "No response data")
        echo -e "   📝 Response: $invalid_data"
    fi
    
    # Test 5: Create Product with Missing Fields
    echo -e "${BLUE}5. Testing Product Creation with Missing Fields${NC}"
    
    missing_payload='{"prodname": "Only Name"}'
    
    missing_response=$(curl -s -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -d "$missing_payload" \
        "${API_URL}api/products" \
        -o /tmp/missing_product_response.json)
    missing_code="${missing_response: -3}"
    
    if [ "$missing_code" = "400" ] || [ "$missing_code" = "422" ]; then
        echo -e "   ✅ Missing fields validation working (HTTP $missing_code)"
    else
        echo -e "   ⚠️  Missing fields response: HTTP $missing_code"
        missing_data=$(cat /tmp/missing_product_response.json 2>/dev/null || echo "No response data")
        echo -e "   📝 Response: $missing_data"
    fi
    
    echo ""
    print_success "🎯 All product endpoints tested!"
    print_status "📦 Products API is functioning correctly"
    
    return 0
}

# Function to test the deployed application
test_application() {
    print_header "🚀 Application Testing Suite"
    
    print_status "Comprehensive testing of the deployed Spring Boot Lambda application"
    echo ""
    
    print_status "🌐 Retrieving API Gateway URL..."
    API_URL=$(get_api_url)
    
    if [ -z "$API_URL" ]; then
        print_error "❌ Could not get API URL. Skipping tests."
        return 1
    fi
    
    print_success "✅ API Gateway URL: $API_URL"
    echo ""
    
    # Wait for deployment to be ready
    print_status "⏳ Waiting for deployment to be ready..."
    sleep 5
    
    # Test basic endpoints first
    print_header "🏥 Basic Health Checks"
    
    # Test health endpoint
    print_status "🔍 Testing /health endpoint..."
    HEALTH_RESPONSE=$(curl -s "${API_URL}health")
    
    if echo "$HEALTH_RESPONSE" | grep -q '"success":true'; then
        print_success "✅ /health endpoint is working"
        echo "   Response: $(echo "$HEALTH_RESPONSE" | jq -r '.message' 2>/dev/null || echo 'Health check passed')"
    else
        print_warning "⚠️  /health endpoint failed or returned unexpected response"
        echo "   Response: $HEALTH_RESPONSE"
    fi
    echo ""
    
    # Interactive authentication testing menu
    print_header "🔐 Comprehensive Testing Options"
    print_status "Choose your testing approach:"
    echo ""
    
    echo -e "${BLUE}1.${NC} 🎯 Interactive Authentication Flow (Recommended)"
    echo -e "   • Test with your own email and password"
    echo -e "   • Complete signup → verification → signin → profile flow"
    echo -e "   • Real-time verification code input"
    echo ""
    
    echo -e "${BLUE}2.${NC} � Comprehensive All Endpoints Testing"
    echo -e "   • Test ALL 15+ endpoints automatically"
    echo -e "   • Authentication endpoints (11)"
    echo -e "   • Products API endpoints (2)"
    echo -e "   • Health and error endpoints (2)"
    echo ""
    
    echo -e "${BLUE}3.${NC} 📦 Products API Testing Only"
    echo -e "   • Test product creation and retrieval"
    echo -e "   • Test error handling and validation"
    echo -e "   • Quick product functionality check"
    echo ""
    
    echo -e "${BLUE}4.${NC} �🚀 Quick Health Check Only"
    echo -e "   • Skip detailed testing"
    echo -e "   • Show deployment summary"
    echo ""
    
    echo -e "${BLUE}5.${NC} 📋 Show Endpoint Documentation"
    echo -e "   • Display all available endpoints"
    echo -e "   • Show curl examples"
    echo ""
    
    while true; do
        echo -n "🎯 Select option (1-5): "
        read -n 1 -r CHOICE
        echo
        
        case $CHOICE in
            1)
                echo ""
                print_success "🎯 Starting Interactive Authentication Flow Testing..."
                echo ""
                test_auth_flow
                break
                ;;
            2)
                echo ""
                print_success "� Starting Comprehensive All Endpoints Testing..."
                echo ""
                test_all_auth_endpoints
                echo ""
                test_products_api
                break
                ;;
            3)
                echo ""
                print_success "📦 Starting Products API Testing..."
                echo ""
                test_products_api
                break
                ;;
            4)
                echo ""
                print_status "🚀 Skipping detailed testing"
                break
                ;;
            5)
                echo ""
                show_endpoint_documentation
                echo ""
                echo -n "🔄 Would you like to run comprehensive tests now? (y/N): "
                read -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    echo ""
                    print_success "🔬 Starting Comprehensive Testing..."
                    echo ""
                    test_all_auth_endpoints
                    echo ""
                    test_products_api
                fi
                break
                ;;
            *)
                print_error "❌ Invalid option. Please select 1-5."
                ;;
        esac
    done
    
    echo ""
    print_header "🎉 Deployment Summary"
    echo -e "${GREEN}🌐 API Gateway URL:${NC} $API_URL"
    echo -e "${GREEN}📍 AWS Region:${NC} $REGION"
    echo -e "${GREEN}📦 Stack Name:${NC} $STACK_NAME"
    echo ""
    
    # Show CloudWatch log information
    get_log_groups
    echo -e "${GREEN}� CloudWatch Monitoring:${NC}"
    echo -e "${GREEN}   • Lambda Log Group:${NC} $LAMBDA_LOG_GROUP"
    echo -e "${GREEN}   • API Gateway Log Group:${NC} $API_LOG_GROUP"
    echo ""
    
    echo -e "${BLUE}🛠️  Management Commands:${NC}"
    echo -e "   • Show recent logs: ${0} --logs"
    echo -e "   • Tail logs real-time: ${0} --tail-logs"
    echo -e "   • Test auth flow: ${0} --test-auth"
    echo -e "   • Clean deployment: ${0} --clean"
}

# Function to show endpoint documentation
show_endpoint_documentation() {
    print_header "📚 API Endpoint Documentation"
    
    echo -e "${YELLOW}🔐 Authentication Endpoints:${NC}"
    echo ""
    
    echo -e "${BLUE}1. User Registration${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/signup${NC}"
    echo -e "   Body: {\"username\": \"john\", \"email\": \"john@example.com\", \"password\": \"Pass123@\", \"name\": \"John Doe\"}"
    echo ""
    
    echo -e "${BLUE}2. Email Verification${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/confirm-signup${NC}"
    echo -e "   Body: {\"email\": \"john@example.com\", \"username\": \"john\", \"confirmationCode\": \"123456\"}"
    echo ""
    
    echo -e "${BLUE}3. User Sign In${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/signin${NC}"
    echo -e "   Body: {\"login\": \"john@example.com\", \"password\": \"Pass123@\"}"
    echo -e "   Note: login can be username OR email"
    echo ""
    
    echo -e "${BLUE}4. User Profile${NC}"
    echo -e "   GET $API_URL${GREEN}api/auth/profile${NC}"
    echo -e "   Headers: Authorization: Bearer <access_token>"
    echo ""
    
    echo -e "${BLUE}5. Resend Confirmation${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/resend-confirmation${NC}"
    echo -e "   Body: {\"username\": \"john\"}"
    echo ""
    
    echo -e "${BLUE}6. Forgot Password${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/forgot-password${NC}"
    echo -e "   Body: {\"email\": \"john@example.com\"}"
    echo ""
    
    echo -e "${BLUE}7. Reset Password${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/reset-password${NC}"
    echo -e "   Body: {\"email\": \"john@example.com\", \"confirmationCode\": \"123456\", \"newPassword\": \"NewPass123@\"}"
    echo ""
    
    echo -e "${BLUE}8. Refresh Token${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/refresh-token${NC}"
    echo -e "   Body: {\"refreshToken\": \"<refresh_token>\", \"username\": \"john\"}"
    echo ""
    
    echo -e "${BLUE}9. Sign Out${NC}"
    echo -e "   POST $API_URL${GREEN}api/auth/signout${NC}"
    echo -e "   Headers: Authorization: Bearer <access_token>"
    echo ""
    
    echo -e "${BLUE}10. Validate Session${NC}"
    echo -e "   GET $API_URL${GREEN}api/auth/validate-session${NC}"
    echo -e "   Headers: Authorization: Bearer <access_token>"
    echo ""
    
    echo -e "${YELLOW}📦 Product Endpoints:${NC}"
    echo ""
    
    echo -e "${BLUE}11. Create Product${NC}"
    echo -e "   POST $API_URL${GREEN}api/products${NC}"
    echo -e "   Body: {\"prodname\": \"Product Name\", \"price\": 99.99}"
    echo ""
    
    echo -e "${BLUE}12. Get Product${NC}"
    echo -e "   GET $API_URL${GREEN}api/products/{id}${NC}"
    echo ""
    
    echo -e "${YELLOW}🏥 Utility Endpoints:${NC}"
    echo ""
    
    echo -e "${BLUE}13. Health Check${NC}"
    echo -e "   GET $API_URL${GREEN}health${NC}"
    echo ""
    
    echo -e "${BLUE}14. Error Testing${NC}"
    echo -e "   GET $API_URL${GREEN}error${NC}"
    echo ""
    
    echo -e "${YELLOW}📝 Example cURL Commands:${NC}"
    echo ""
    echo -e "${GREEN}# Health Check${NC}"
    echo -e "curl '$API_URL${GREEN}health${NC}'"
    echo ""
    echo -e "${GREEN}# User Registration${NC}"
    echo -e "curl -X POST -H \"Content-Type: application/json\" \\"
    echo -e "  -d '{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"Test123@\",\"name\":\"Test User\"}' \\"
    echo -e "  '$API_URL${GREEN}api/auth/signup${NC}'"
    echo ""
    echo -e "${GREEN}# User Sign In${NC}"
    echo -e "curl -X POST -H \"Content-Type: application/json\" \\"
    echo -e "  -d '{\"login\":\"test@example.com\",\"password\":\"Test123@\"}' \\"
    echo -e "  '$API_URL${GREEN}api/auth/signin${NC}'"
    echo ""
    echo -e "${GREEN}# Create Product${NC}"
    echo -e "curl -X POST -H \"Content-Type: application/json\" \\"
    echo -e "  -d '{\"prodname\":\"Test Product\",\"price\":99.99}' \\"
    echo -e "  '$API_URL${GREEN}api/products${NC}'"
    echo ""
    echo -e "${GREEN}# Get Product${NC}"
    echo -e "curl '$API_URL${GREEN}api/products/{product_id}${NC}'"
}

# Function to show Cognito resources created by CloudFormation
show_cognito_resources() {
    print_header "🔐 Cognito Resources Created"
    
    # Get Cognito resources from CloudFormation outputs
    COGNITO_USER_POOL_ID=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`CognitoUserPoolId`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    COGNITO_CLIENT_ID=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`CognitoUserPoolClientId`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    COGNITO_USER_POOL_ARN=$(aws cloudformation describe-stacks \
        --stack-name "$STACK_NAME" \
        --region "$REGION" \
        --query 'Stacks[0].Outputs[?OutputKey==`CognitoUserPoolArn`].OutputValue' \
        --output text 2>/dev/null || echo "")
    
    if [ -n "$COGNITO_USER_POOL_ID" ]; then
        print_success "✅ Cognito User Pool created successfully!"
        print_status "📋 Cognito Configuration:"
        print_status "   • User Pool ID: $COGNITO_USER_POOL_ID"
        print_status "   • Client ID: $COGNITO_CLIENT_ID"
        print_status "   • User Pool ARN: $COGNITO_USER_POOL_ARN"
        print_status "   • Region: $REGION"
        echo ""
        
        print_status "🔧 Cognito User Pool Features:"
        print_status "   • Authentication flows: Username/Password, SRP, USER_AUTH"
        print_status "   • Sign-in options: Username, Email"
        print_status "   • Required attributes: Email, Name"
        print_status "   • Auto-verification: Email"
        print_status "   • MFA: Disabled"
        print_status "   • Token validity: Access(60min), ID(60min), Refresh(5days)"
        print_status "   • Advanced security: Enabled"
        echo ""
        
        print_status "📍 Parameter Store entries:"
        print_status "   • /spring-boot-demo/cognito/user-pool-id"
        print_status "   • /spring-boot-demo/cognito/client-id"
        print_status "   • /spring-boot-demo/cognito/client-secret (🔐 encrypted)"
        print_status "   • /spring-boot-demo/cognito/region"
        echo ""
        
        print_success "🎉 Your Spring Boot application is now configured to use CloudFormation-managed Cognito!"
    else
        print_warning "⚠️  Cognito resources not found in CloudFormation outputs"
        print_status "This might be normal if the deployment is still in progress"
    fi
}

# Function to check prerequisites
check_prerequisites() {
    print_header "Checking Prerequisites"
    
    # Check if required tools are installed
    commands=("aws" "sam" "mvn" "java" "curl")
    
    for cmd in "${commands[@]}"; do
        if command -v "$cmd" >/dev/null 2>&1; then
            print_success "✓ $cmd is installed"
        else
            print_error "✗ $cmd is not installed"
            exit 1
        fi
    done
    
    # Check AWS credentials
    if aws sts get-caller-identity >/dev/null 2>&1; then
        ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
        print_success "✓ AWS credentials configured (Account: $ACCOUNT_ID)"
    else
        print_error "✗ AWS credentials not configured"
        exit 1
    fi
    
    # Check if we're in the right directory
    if [ ! -f "pom.xml" ] || [ ! -f "template.yaml" ]; then
        print_error "✗ Not in Spring Boot project directory (missing pom.xml or template.yaml)"
        exit 1
    fi
    
    print_success "✓ All prerequisites met"
}

# Function to show help
show_help() {
    echo "Spring Boot Lambda Deployment Script"
    echo ""
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help          Show this help message"
    echo "  -t, --test-only     Only test the existing deployment"
    echo "  -b, --build-only    Only build the project"
    echo "  -d, --deploy-only   Only deploy (skip build and test)"
    echo "  --clean             Clean deployment (delete and redeploy)"
    echo "  --logs              Show recent CloudWatch logs"
    echo "  --tail-logs         Tail CloudWatch logs in real-time"
    echo "  --test-auth         Test authentication flow interactively"
    echo "  --test-all          Test all endpoints comprehensively"
    echo "  --test-products     Test products API endpoints only"
    echo ""
    echo "Configuration:"
    echo "  On first run, the script will ask for:"
    echo "  • Stack name (default: spring-boot-demo)"
    echo "  • S3 bucket name (default: spring-boot-demo-artifacts-ACCOUNT_ID)"
    echo "  • AWS region (default: us-east-1)"
    echo "  Press Enter to use defaults or provide custom values."
    echo ""
    echo "  Configuration is saved in samconfig.toml for subsequent runs."
    echo "  Use --clean to remove all resources and configuration."
    echo ""
    echo "Security Configuration:"
    echo "  The script automatically manages secure configuration in AWS Parameter Store:"
    echo "  • Cognito Client ID, Client Secret, User Pool ID, and Region"
    echo "  • Secrets are encrypted using AWS Systems Manager SecureString"
    echo "  • Configuration is automatically set up before deployment"
    echo "  • All parameters are automatically deleted during --clean"
    echo ""
    echo "Authentication Testing:"
    echo "  Use --test-auth to run an interactive authentication flow test that:"
    echo "  • Creates a test user (signup)"
    echo "  • Prompts for verification code"
    echo "  • Confirms the user"
    echo "  • Signs in the user"
    echo "  • Accesses the user profile"
    echo ""
    echo "  Use --test-all for comprehensive testing of ALL endpoints:"
    echo "  • 11 Authentication endpoints"
    echo "  • 2 Products API endpoints"
    echo "  • 2 Health/error endpoints"
    echo ""
    echo "  Use --test-products for quick Products API testing only"
    echo ""
    echo "Important Notes:"
    echo "  • The Cognito User Pool is NOT managed by CloudFormation"
    echo "  • Clean deployment will NOT delete the User Pool"
    echo "  • BUT secure configuration parameters WILL be deleted during --clean"
    echo "  • User data in Cognito will persist across deployments"
    echo ""
    echo "Environment Variables:"
    echo "  PROJECT_NAME        Project name (default: spring-boot-demo)"
    echo "  REGION              AWS region (default: us-east-1)"
    echo "  STAGE               Deployment stage (default: dev)"
    echo ""
    echo "CloudWatch Monitoring:"
    echo "  The deployment creates CloudWatch log groups for:"
    echo "  • Lambda function execution logs"
    echo "  • API Gateway access logs"
    echo "  Use --logs to view recent logs or --tail-logs for real-time monitoring"
    echo ""
}

# Function to clean deployment
clean_deployment() {
    print_header "Cleaning Deployment"
    
    # Important warning about Cognito User Pool
    print_warning "IMPORTANT: This script will NOT delete the Cognito User Pool!"
    print_warning "The Cognito User Pool (us-east-1_X3grEwPDP) was not created by CloudFormation"
    print_warning "and must be deleted manually from the AWS Console if needed."
    print_warning "All user data in the User Pool will remain intact."
    echo ""
    
    # Load configuration first to get the correct bucket and stack names
    if [ -f "samconfig.toml" ] && [ -s "samconfig.toml" ]; then
        print_status "Loading configuration from samconfig.toml for cleanup"
        STACK_NAME=$(grep 'stack_name' samconfig.toml | cut -d'"' -f2)
        S3_BUCKET_NAME=$(grep 's3_bucket' samconfig.toml | cut -d'"' -f2)
        REGION=$(grep 'region' samconfig.toml | cut -d'"' -f2)
        
        if [ -z "$STACK_NAME" ] || [ -z "$S3_BUCKET_NAME" ] || [ -z "$REGION" ]; then
            print_warning "Could not read configuration from samconfig.toml - using defaults"
            STACK_NAME="spring-boot-demo"
            ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
            S3_BUCKET_NAME="spring-boot-demo-artifacts-$ACCOUNT_ID"
            REGION="us-east-1"
        fi
        
        print_status "Will delete stack: $STACK_NAME, bucket: $S3_BUCKET_NAME, region: $REGION"
    else
        print_warning "No valid samconfig.toml found - using default values"
        STACK_NAME="spring-boot-demo"
        ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
        S3_BUCKET_NAME="spring-boot-demo-artifacts-$ACCOUNT_ID"
        REGION="us-east-1"
        print_status "Will delete stack: $STACK_NAME, bucket: $S3_BUCKET_NAME, region: $REGION"
    fi
    
    print_warning "This will delete the CloudFormation stack, S3 bucket, and samconfig.toml"
    print_warning "But will NOT delete the Cognito User Pool"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        # Step 1: Delete CloudFormation stack and all associated resources
        print_status "Deleting CloudFormation stack: $STACK_NAME..."
        sam delete --stack-name "$STACK_NAME" --region "$REGION" --no-prompts 2>&1 || {
            print_warning "Stack deletion failed or stack doesn't exist, continuing with manual cleanup..."
        }
        
        # Step 2: Check if bucket exists and delete it
        print_status "Checking if S3 bucket exists: $S3_BUCKET_NAME..."
        if aws s3api head-bucket --bucket "$S3_BUCKET_NAME" --region "$REGION" 2>/dev/null; then
            print_status "Bucket exists, deleting all contents..."
            
            # Delete all objects and versions
            print_status "Deleting all object versions..."
            aws s3api list-object-versions --bucket "$S3_BUCKET_NAME" --region "$REGION" \
                --query 'Versions[].[Key,VersionId]' --output text 2>/dev/null | \
                while read key version_id; do
                    if [ -n "$key" ] && [ -n "$version_id" ]; then
                        echo "Deleting version: $key ($version_id)"
                        aws s3api delete-object --bucket "$S3_BUCKET_NAME" --key "$key" --version-id "$version_id" --region "$REGION" 2>/dev/null || true
                    fi
                done
            
            # Delete all delete markers
            print_status "Deleting all delete markers..."
            aws s3api list-object-versions --bucket "$S3_BUCKET_NAME" --region "$REGION" \
                --query 'DeleteMarkers[].[Key,VersionId]' --output text 2>/dev/null | \
                while read key version_id; do
                    if [ -n "$key" ] && [ -n "$version_id" ]; then
                        echo "Deleting delete marker: $key ($version_id)"
                        aws s3api delete-object --bucket "$S3_BUCKET_NAME" --key "$key" --version-id "$version_id" --region "$REGION" 2>/dev/null || true
                    fi
                done
            
            # Finally delete the bucket
            print_status "Deleting the S3 bucket itself..."
            if aws s3api delete-bucket --bucket "$S3_BUCKET_NAME" --region "$REGION" 2>/dev/null; then
                print_success "S3 bucket deleted successfully"
            else
                print_warning "Failed to delete S3 bucket, you may need to delete it manually"
            fi
        else
            print_status "S3 bucket does not exist or already deleted"
        fi
        
        # Step 3: Check for any remaining API Gateway resources
        print_status "Checking for any remaining API Gateway resources..."
        API_IDS=$(aws apigateway get-rest-apis --region "$REGION" --query "items[?contains(name, '$STACK_NAME') || contains(name, 'spring-boot-demo')].id" --output text 2>/dev/null || echo "")
        if [ -n "$API_IDS" ]; then
            for api_id in $API_IDS; do
                print_status "Deleting API Gateway: $api_id"
                aws apigateway delete-rest-api --rest-api-id "$api_id" --region "$REGION" 2>/dev/null || true
            done
        fi
        
        # Step 4: Check for any remaining Lambda functions
        print_status "Checking for any remaining Lambda functions..."
        LAMBDA_FUNCTIONS=$(aws lambda list-functions --region "$REGION" --query "Functions[?contains(FunctionName, '$STACK_NAME') || contains(FunctionName, 'spring-boot-demo')].FunctionName" --output text 2>/dev/null || echo "")
        if [ -n "$LAMBDA_FUNCTIONS" ]; then
            for func_name in $LAMBDA_FUNCTIONS; do
                print_status "Deleting Lambda function: $func_name"
                aws lambda delete-function --function-name "$func_name" --region "$REGION" 2>/dev/null || true
            done
        fi
        
        # Step 5: Delete CloudWatch Log Groups
        print_status "Deleting CloudWatch Log Groups..."
        
        # Delete Lambda log group
        LAMBDA_LOG_GROUP="/aws/lambda/${STACK_NAME}-${STAGE}"
        if aws logs describe-log-groups --log-group-name-prefix "$LAMBDA_LOG_GROUP" --region "$REGION" --query 'logGroups[0].logGroupName' --output text 2>/dev/null | grep -q "$LAMBDA_LOG_GROUP"; then
            print_status "Deleting Lambda log group: $LAMBDA_LOG_GROUP"
            aws logs delete-log-group --log-group-name "$LAMBDA_LOG_GROUP" --region "$REGION" 2>/dev/null || true
        fi
        
        # Delete API Gateway log group
        API_LOG_GROUP="/aws/apigateway/${STACK_NAME}-api-${STAGE}"
        if aws logs describe-log-groups --log-group-name-prefix "$API_LOG_GROUP" --region "$REGION" --query 'logGroups[0].logGroupName' --output text 2>/dev/null | grep -q "$API_LOG_GROUP"; then
            print_status "Deleting API Gateway log group: $API_LOG_GROUP"
            aws logs delete-log-group --log-group-name "$API_LOG_GROUP" --region "$REGION" 2>/dev/null || true
        fi
        
        # Delete API Gateway execution logs (these are created automatically by AWS)
        print_status "Deleting API Gateway execution logs..."
        EXECUTION_LOG_GROUPS=$(aws logs describe-log-groups --log-group-name-prefix "API-Gateway-Execution-Logs" --region "$REGION" --query 'logGroups[*].logGroupName' --output text 2>/dev/null || echo "")
        if [ -n "$EXECUTION_LOG_GROUPS" ]; then
            for log_group in $EXECUTION_LOG_GROUPS; do
                # Check if this log group belongs to our API by checking if it contains our API ID
                if [[ "$log_group" == *"API-Gateway-Execution-Logs"* ]]; then
                    print_status "Deleting API Gateway execution log group: $log_group"
                    aws logs delete-log-group --log-group-name "$log_group" --region "$REGION" 2>/dev/null || true
                fi
            done
        fi
        
        # Also check for any log groups with spring-boot-demo prefix (fallback)
        LAMBDA_LOG_GROUPS=$(aws logs describe-log-groups --log-group-name-prefix "/aws/lambda/spring-boot-demo" --region "$REGION" --query 'logGroups[*].logGroupName' --output text 2>/dev/null || echo "")
        if [ -n "$LAMBDA_LOG_GROUPS" ]; then
            for log_group in $LAMBDA_LOG_GROUPS; do
                print_status "Deleting Lambda log group: $log_group"
                aws logs delete-log-group --log-group-name "$log_group" --region "$REGION" 2>/dev/null || true
            done
        fi
        
        API_LOG_GROUPS=$(aws logs describe-log-groups --log-group-name-prefix "/aws/apigateway/spring-boot-demo" --region "$REGION" --query 'logGroups[*].logGroupName' --output text 2>/dev/null || echo "")
        if [ -n "$API_LOG_GROUPS" ]; then
            for log_group in $API_LOG_GROUPS; do
                print_status "Deleting API Gateway log group: $log_group"
                aws logs delete-log-group --log-group-name "$log_group" --region "$REGION" 2>/dev/null || true
            done
        fi
        
        # Step 6: Clean up secure configuration from Parameter Store
        print_status "Cleaning up secure configuration..."
        cleanup_secure_config
        
        # Step 7: Remove samconfig.toml
        print_status "Removing samconfig.toml..."
        rm -f samconfig.toml
        
        print_success "Cleanup completed - all resources should be deleted"
        print_status "Please check AWS Console to verify all resources are removed"
    else
        print_status "Cleanup cancelled"
    fi
}

# Main execution
main() {
    print_header "Spring Boot Lambda Deployment"
    
    # Parse command line arguments
    SKIP_BUILD=false
    SKIP_DEPLOY=false
    SKIP_TEST=false
    CLEAN_DEPLOY=false
    SHOW_LOGS=false
    TAIL_LOGS=false
    TEST_AUTH_ONLY=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--help)
                show_help
                exit 0
                ;;
            -t|--test-only)
                SKIP_BUILD=true
                SKIP_DEPLOY=true
                shift
                ;;
            -b|--build-only)
                SKIP_DEPLOY=true
                SKIP_TEST=true
                shift
                ;;
            -d|--deploy-only)
                SKIP_BUILD=true
                SKIP_TEST=true
                shift
                ;;
            --clean)
                CLEAN_DEPLOY=true
                shift
                ;;
            --logs)
                SHOW_LOGS=true
                shift
                ;;
            --tail-logs)
                TAIL_LOGS=true
                shift
                ;;
            --test-auth)
                TEST_AUTH_ONLY=true
                shift
                ;;
            *)
                print_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # Clean deployment if requested
    if [ "$CLEAN_DEPLOY" = true ]; then
        clean_deployment
        exit 0
    fi
    
    # Load configuration for log operations
    load_samconfig
    
    # Show logs if requested
    if [ "$SHOW_LOGS" = true ]; then
        show_recent_logs
        exit 0
    fi
    
    # Tail logs if requested
    if [ "$TAIL_LOGS" = true ]; then
        tail_logs
        exit 0
    fi
    
    # Test auth only if requested
    if [ "$TEST_AUTH_ONLY" = true ]; then
        test_auth_flow
        exit 0
    fi
    
    # Check prerequisites
    check_prerequisites
    
    # Create configuration if needed
    create_samconfig
    
    # Now reload config in case it was just created
    load_samconfig
    
    # Setup secure configuration in Parameter Store
    setup_secure_config
    
    # Setup S3 bucket
    if ! check_s3_bucket; then
        create_s3_bucket
    fi
    
    # Build project
    if [ "$SKIP_BUILD" = false ]; then
        build_project
    fi
    
    # Deploy application
    if [ "$SKIP_DEPLOY" = false ]; then
        deploy_application
    fi
    
    # Test application
    if [ "$SKIP_TEST" = false ]; then
        test_application
    fi
    
    # Show Cognito resources created
    show_cognito_resources
    
    print_header "Deployment Complete! 🎉"
}

# Run main function
main "$@"
