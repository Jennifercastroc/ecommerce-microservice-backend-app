pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Xmx512m -XX:+UseG1GC'
        DOCKER_COMPOSE = 'docker compose --project-name ecommerce-ci'
        COMPOSE_FILES = '-f core.yml -f compose.yml -f compose.ci.yml'
        CI_GATEWAY_PORT = '18080'
        CI_CLOUD_CONFIG_PORT = '19296'
        CI_SERVICE_DISCOVERY_PORT = '18761'
        CI_ZIPKIN_PORT = '19411'
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

        stage('Build microservice') {
            steps {
                dir(params.MICROSERVICE) {
                    sh 'mvn -B clean package -Dmaven.test.skip=true'
                }
            }
        }

        stage('Docker build') {
            steps {
                script {
                    def imageName = "${params.DOCKER_REGISTRY}/${params.MICROSERVICE}-ecommerce-boot"
                    def stageTag = env.BRANCH_NAME ? env.BRANCH_NAME.replaceAll('/', '-') : 'local'
                    def buildTag = "${imageName}:${stageTag}-${env.BUILD_NUMBER}"
                    def releaseTag = "${imageName}:${params.PROJECT_VERSION}"

                    dir(params.MICROSERVICE) {
                        sh "docker build -t ${buildTag} -t ${releaseTag} ."
                    }

                    env.IMAGE_NAME = imageName
                    env.IMAGE_BUILD_TAG = buildTag
                    env.IMAGE_RELEASE_TAG = releaseTag
                }
            }
        }

        stage('Deploy stage stack') {
            when {
                expression { env.BRANCH_NAME?.startsWith('stage/') }
            }
            steps {
                script {
                    sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} down --remove-orphans || true"
                    sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} up -d --remove-orphans"
                }
            }
        }

        stage('E2E tests') {
            when {
                expression { env.BRANCH_NAME?.startsWith('stage/') }
            }
            steps {
                withEnv(["API_GATEWAY_BASE_URL=http://localhost:${CI_GATEWAY_PORT}"]) {
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
                    sh "${DOCKER_COMPOSE} ${COMPOSE_FILES} down --remove-orphans || true"
                }
            }
        }
    }
}
