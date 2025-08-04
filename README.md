# AWS Spring Boot Serverless Template

A boilerplate project to kickstart your **Spring Boot** applications with **AWS Serverless** architecture. This template is tailored for developers looking to rapidly build, test, and deploy Spring Boot applications using **AWS Lambda**, **API Gateway**, and **CloudFormation** — all with minimal setup.

## 🚀 About This Project

This project provides a fully functional template to deploy Spring Boot applications on AWS Lambda using API Gateway as the HTTP interface. It is ideal for developers and DevOps engineers who want a scalable, cost-effective, and event-driven backend using Java and Spring Boot without managing traditional servers.

Use this template to:

- Build stateless Spring Boot APIs
- Automatically provision infrastructure using AWS SAM and CloudFormation
- Handle deployments via a streamlined `deploy.sh` script

## 📚 Documentation

This template integrates the following AWS services:

- **AWS Lambda** – Serverless compute for running backend logic
- **API Gateway** – Expose RESTful APIs to users
- **CloudFormation** – Define and manage infrastructure as code

## 🔌 API Reference

### `GET /health`

Performs a health check on the deployed Spring Boot application.

**Request:**

```http
GET /health
```

**Response:**

```json
{
  "message": "Health check successful",
  "status": 0,
  "success": true
}
```

## 🧪 Demo & Deployment

You can build, test, and deploy your Spring Boot application using the included `deploy.sh` script, which simplifies the entire lifecycle of your serverless app.

### ✅ Usage

```bash
./deploy.sh [OPTIONS]
```

### 🔧 Available Options

| Option          | Description                            |
| --------------- | -------------------------------------- |
| `-h`, `--help`  | Show help message                      |
| `-t`, `--test`  | Test the existing deployment           |
| `-b`, `--build` | Only build the project (no deployment) |
| `-s`, `--setup` | Configure deployment settings only     |
| `-c`, `--clean` | Clean up all AWS resources             |
| `--logs`        | Show recent CloudWatch logs            |
| `--tail-logs`   | Tail CloudWatch logs in real-time      |

### ⚙️ Configuration

On the first run, the script will interactively prompt for:

- **Stack Name** (default: `spring-boot-demo`)
  - Lambda Function: `{stack-name}-func`
  - API Gateway: `{stack-name}-api`
  - S3 Bucket: `{stack-name}-artifacts-{account-id}`
- **AWS Region** (default: `us-east-1`)

These values are saved in `samconfig.toml` for future runs.
Use `--clean` to delete all deployed resources and reset the configuration.

### 🌍 Environment Variables

You can override default behavior using environment variables:

| Variable     | Description                            | Default            |
| ------------ | -------------------------------------- | ------------------ |
| `STACK_NAME` | The name of the CloudFormation stack   | `spring-boot-demo` |
| `REGION`     | AWS Region to deploy to                | `us-east-1`        |
| `STAGE`      | Deployment stage (e.g., `dev`, `prod`) | `dev`              |

### 📊 CloudWatch Monitoring

This deployment automatically configures **CloudWatch** log groups for enhanced observability:

- 📄 **Lambda Function Execution Logs**
- 🌐 **API Gateway Access Logs**

Use the following options to inspect logs:

- `--logs`: View recent logs for deployed resources
- `--tail-logs`: Continuously stream logs in real-time

These logs are crucial for debugging and monitoring your serverless application post-deployment.

## 👨‍💻 Author

Made with ❤️ by [@xanderbilla](https://www.github.com/xanderbilla)
