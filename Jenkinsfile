pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Xmx512m -XX:+UseG1GC'
        DOCKER_COMPOSE = 'docker compose --project-name ecommerce-ci'
        COMPOSE_FILES = '-f core.yml -f compose.yml -f compose.ci.yml'
        CI_GATEWAY_PORT = '18080'
        CI_CLOUD_CONFIG_PORT = '29296'
        CI_SERVICE_DISCOVERY_PORT = '28761'
        CI_ZIPKIN_PORT = '29411'
    }

    parameters {
        string(name: 'MICROSERVICE', defaultValue: 'product-service', description: 'Nombre del modulo a construir (carpeta con pom y Dockerfile).')
        string(name: 'PROJECT_VERSION', defaultValue: '0.1.0', description: 'Version del artefacto utilizada en los Dockerfiles / docker-compose.')
        string(name: 'DOCKER_REGISTRY', defaultValue: 'selimhorri', description: 'Repositorio Docker donde etiquetar la imagen.')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Setup tooling') {
            steps {
                sh '''
                    set -euxo pipefail

                    TOOLS_DIR="$HOME/.local/bin"
                    mkdir -p "$TOOLS_DIR"
                    if ! [ -x "$TOOLS_DIR/docker" ]; then
                        DOCKER_VERSION="24.0.7"
                        curl -fsSL "https://download.docker.com/linux/static/stable/x86_64/docker-${DOCKER_VERSION}.tgz" -o /tmp/docker.tgz
                        tar -xzf /tmp/docker.tgz --strip-components=1 -C "$TOOLS_DIR" docker/docker
                        rm -f /tmp/docker.tgz
                    fi

                    PLUGIN_DIR="$HOME/.docker/cli-plugins"
                    mkdir -p "$PLUGIN_DIR"
                    if ! [ -x "$PLUGIN_DIR/docker-compose" ]; then
                        COMPOSE_VERSION="2.24.6"
                        curl -fsSL "https://github.com/docker/compose/releases/download/v${COMPOSE_VERSION}/docker-compose-linux-x86_64" -o "$PLUGIN_DIR/docker-compose"
                        chmod +x "$PLUGIN_DIR/docker-compose"
                    fi

                    if [ -f "./mvnw" ]; then
                        chmod +x ./mvnw
                    fi
                '''
            }
        }

        stage('Build microservice') {
            steps {
                script {
                    def pathWithTools = "${env.HOME}/.local/bin:${env.PATH}"
                    def mainClass = "com.selimhorri.app.${params.MICROSERVICE.split('-').collect { it.capitalize() }.join('')}Application"

                    withEnv(["PATH=${pathWithTools}"]) {
                        dir(params.MICROSERVICE) {
                            sh "./mvnw -B clean package -Dmaven.test.skip=true -Dspring-boot.repackage.mainClass=${mainClass}"
                        }
                    }

                    def imageName = "${params.DOCKER_REGISTRY}/${params.MICROSERVICE}-ecommerce-boot"
                    def stageTag = env.BRANCH_NAME ? env.BRANCH_NAME.replaceAll('/', '-') : 'local'
                    env.IMAGE_NAME = imageName
                    env.IMAGE_BUILD_TAG = "${imageName}:${stageTag}-${env.BUILD_NUMBER}"
                    env.IMAGE_RELEASE_TAG = "${imageName}:${params.PROJECT_VERSION}"
                }
            }
        }

        stage('Docker build') {
            steps {
                script {
                    def pathWithTools = "${env.HOME}/.local/bin:${env.PATH}"
                    withEnv(["PATH=${pathWithTools}"]) {
                        dir(params.MICROSERVICE) {
                            sh "docker build -t ${env.IMAGE_BUILD_TAG} -t ${env.IMAGE_RELEASE_TAG} ."
                        }
                    }
                }
            }
        }

        stage('Deploy stage stack') {
            when {
                expression { env.BRANCH_NAME?.startsWith('stage/') }
            }
            steps {
                script {
                    def pathWithTools = "${env.HOME}/.local/bin:${env.PATH}"
                    withEnv(["PATH=${pathWithTools}"]) {
                        sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} down --remove-orphans || true"
                        sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} up -d --remove-orphans"
                    }
                }
            }
        }

        stage('E2E tests') {
            when {
                expression { env.BRANCH_NAME?.startsWith('stage/') }
            }
            steps {
                withEnv([
                    "API_GATEWAY_BASE_URL=http://localhost:${CI_GATEWAY_PORT}",
                    "PATH=${env.HOME}/.local/bin:${env.PATH}"
                ]) {
                    sh '''
                        python -m pip install --upgrade pip
                        python -m pip install pytest requests locust
                        mkdir -p build/reports
                        pytest -m e2e tests/e2e --junitxml=build/reports/e2e-results.xml --maxfail=1
                    '''
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/reports/e2e-results.xml'
                }
            }
        }
    }

    post {
        always {
            script {
                if (env.BRANCH_NAME?.startsWith('stage/')) {
                    def pathWithTools = "${env.HOME}/.local/bin:${env.PATH}"
                    withEnv(["PATH=${pathWithTools}"]) {
                        sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} down --remove-orphans || true"
                    }
                }
            }
        }
    }
}
