pipeline {
    agent any
    
    environment {
        // 项目配置
        PROJECT_NAME = 'ums-server'
        PROJECT_VERSION = '2.0'
        DOCKER_IMAGE_NAME = 'ums-server'
        DOCKER_IMAGE_TAG = "${PROJECT_VERSION}"
        
        // Maven配置
        MAVEN_HOME = tool 'Maven-3.8.6'
        JAVA_HOME = tool 'JDK-8'
        
        // 构建产物
        JAR_FILE = "target/ums-0.0.1-SNAPSHOT.jar"
        DOCKER_FILE = "Dockerfile"
        
        // 部署配置
        DEPLOY_HOST = 'your-deploy-server'
        DEPLOY_USER = 'deploy'
        DEPLOY_PATH = '/opt/ums-server'
    }
    
    options {
        // 构建历史保留
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // 超时设置
        timeout(time: 30, unit: 'MINUTES')
        // 并行构建限制
        disableConcurrentBuilds()
    }
    
    stages {
        stage('代码检出') {
            steps {
                script {
                    // 清理工作空间
                    cleanWs()
                    
                    // 检出代码
                    checkout scm
                    
                    echo "代码检出完成，分支: ${env.BRANCH_NAME}"
                }
            }
        }
        
        stage('代码质量检查') {
            parallel {
                stage('SonarQube分析') {
                    when {
                        anyOf {
                            branch 'main'
                            branch 'master'
                            branch 'develop'
                        }
                    }
                    steps {
                        script {
                            withSonarQubeEnv('SonarQube') {
                                sh "${MAVEN_HOME}/bin/mvn clean verify sonar:sonar \
                                    -Dsonar.projectKey=${PROJECT_NAME} \
                                    -Dsonar.projectName=${PROJECT_NAME} \
                                    -Dsonar.projectVersion=${PROJECT_VERSION}"
                            }
                        }
                    }
                }
                
                stage('单元测试') {
                    steps {
                        script {
                            sh "${MAVEN_HOME}/bin/mvn test -DskipTests=false"
                        }
                    }
                    post {
                        always {
                            // 发布测试报告
                            publishTestResults testResultsPattern: '**/target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }
        
        stage('Maven构建') {
            steps {
                script {
                    // 设置Maven环境
                    withEnv(["PATH+MAVEN=${MAVEN_HOME}/bin", "JAVA_HOME=${JAVA_HOME}"]) {
                        // 清理并编译
                        sh "mvn clean compile -DskipTests"
                        
                        // 打包
                        sh "mvn package -DskipTests"
                        
                        // 验证构建产物
                        sh "ls -la ${JAR_FILE}"
                    }
                }
            }
            post {
                success {
                    // 归档构建产物
                    archiveArtifacts artifacts: "${JAR_FILE}", fingerprint: true
                    echo "Maven构建成功，JAR包已归档"
                }
                failure {
                    echo "Maven构建失败"
                }
            }
        }
        
        stage('Docker镜像构建') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                script {
                    // 构建Docker镜像
                    docker.build("${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}")
                    
                    // 标记镜像
                    docker.withRegistry('https://your-registry.com', 'docker-registry-credentials') {
                        docker.image("${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}").push()
                        docker.image("${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}").push('latest')
                    }
                }
            }
            post {
                success {
                    echo "Docker镜像构建成功: ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
                }
                failure {
                    echo "Docker镜像构建失败"
                }
            }
        }
        
        stage('部署到测试环境') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    // 部署到测试环境
                    sshagent(['test-server-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} '
                                cd ${DEPLOY_PATH}
                                docker-compose down
                                docker pull ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}
                                docker-compose up -d
                                docker system prune -f
                            '
                        """
                    }
                }
            }
            post {
                success {
                    echo "测试环境部署成功"
                }
                failure {
                    echo "测试环境部署失败"
                }
            }
        }
        
        stage('部署到生产环境') {
            when {
                anyOf {
                    branch 'main'
                    branch 'master'
                }
            }
            steps {
                script {
                    // 生产环境部署确认
                    timeout(time: 5, unit: 'MINUTES') {
                        input message: '确认部署到生产环境？', ok: '确认部署'
                    }
                    
                    // 部署到生产环境
                    sshagent(['prod-server-ssh-key']) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} '
                                cd ${DEPLOY_PATH}
                                docker-compose down
                                docker pull ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}
                                docker-compose up -d
                                docker system prune -f
                            '
                        """
                    }
                }
            }
            post {
                success {
                    echo "生产环境部署成功"
                }
                failure {
                    echo "生产环境部署失败"
                }
            }
        }
    }
    
    post {
        always {
            // 清理工作空间
            cleanWs()
        }
        success {
            echo "流水线执行成功！"
        }
        failure {
            echo "流水线执行失败！"
        }
        unstable {
            echo "流水线执行不稳定！"
        }
    }
} 