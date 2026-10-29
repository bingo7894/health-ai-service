pipeline {
    agent any

    environment {
        SERVICE_NAME = 'health-ai-service'
        AWS_REGION   = 'ap-southeast-1'
        ECR_REPO     = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${SERVICE_NAME}"
        IMAGE_TAG    = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test & Package') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${SERVICE_NAME}:${IMAGE_TAG} ."
                sh "docker tag ${SERVICE_NAME}:${IMAGE_TAG} ${ECR_REPO}:${IMAGE_TAG}"
                sh "docker tag ${SERVICE_NAME}:${IMAGE_TAG} ${ECR_REPO}:latest"
            }
        }

        stage('Push to Amazon ECR') {
            when {
                branch 'main'
            }
            steps {
                sh "aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REPO}"
                sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
                sh "docker push ${ECR_REPO}:latest"
            }
        }

        stage('Deploy to Amazon ECS') {
            when {
                branch 'main'
            }
            steps {
                sh "aws ecs update-service --cluster health-cluster --service ${SERVICE_NAME} --force-new-deployment --region ${AWS_REGION}"
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully for ${SERVICE_NAME}!"
        }
        failure {
            echo "Pipeline failed for ${SERVICE_NAME}. Please check logs."
        }
    }
}

