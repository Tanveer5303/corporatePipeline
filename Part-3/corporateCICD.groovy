pipeline {
    agent any
    tools{
        jdk 'jdk17'
        maven 'maven3'
    }
    environment{
        SONAR_HOME= tool 'sonar-scanner'
    }
    stages {
        stage('1. Git Checkout') {
            steps {
                git credentialsId: 'GIThub-Creds', url: 'https://github.com/Tanveer5303/BoardGame.git'
            }
        }
        stage('2. Compile Code') {
            steps {
                sh "mvn compile"
            }
        }
        stage('3. Test Cases') {
            steps {
                sh "mvn test"
            }
        }
        stage('4. File System Scan (trivy)') {
            steps {
                sh "trivy fs --format table -o trivy-fs-report.html ."
            }
        }
        stage('5. SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar') {
                 sh '''$SONAR_HOME/bin/sonar-scanner \
                -Dsonar.url=http://65.0.12.10:9000/ \
                -Dsonar.projectName=Boardgames \
                -Dsonar.java.binaries=. \
                -Dsonar.projectKey=Boardgames'''
                }
            }
        }
        stage('6. Quality Gate') {
            steps {
                script{
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token'
                }
            }
        }
        stage('7. Build Application') {
            steps {
                sh "mvn package"
            }
        }
        stage('8. Publish Artifact to Nexus') {
            steps {
                 withMaven(globalMavenSettingsConfig: 'global-settings', jdk: 'jdk17', maven: 'maven3', mavenSettingsConfig: '', traceability: true) {
                sh "mvn deploy -X"
                }
        }
        }
        stage('9. Build & Tag Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'dockerCreds', toolName: 'docker') {
                        sh "docker build -t gravitea/boardgame:latest ."
                    }
                }
            }
        }
        stage('10. Docker Image Scan') {
            steps {
                sh "trivy image --format table -o trivy-image-report.html gravitea/boardgame:latest"
            }
        }
        stage('11. Push Docker Image') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'dockerCreds', toolName: 'docker') {
                        sh "docker push gravitea/boardgame:latest"
                    }
                }
            }
        }
        stage('12. Deploy to Kubernetes') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'k8s-cred', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://172.31.36.223:6443') {
                    sh "kubectl apply -f deployment-service.yaml"
                }
            }
        }
        stage('13. Verify The Deployment') {
            steps {
                withKubeConfig(caCertificate: '', clusterName: 'kubernetes', contextName: '', credentialsId: 'k8s-cred', namespace: 'webapps', restrictKubeConfigAccess: false, serverUrl: 'https://172.31.36.223:6443') {
                    sh "kubectl get pod -n webapps"
                    sh "kubectl get svc -n webapps"
                }
            }
        }
        
    }
    post {
    always {
        script {
            def jobName = env.JOB_NAME
            def buildNumber = env.BUILD_NUMBER
            def pipelineStatus = currentBuild.result ?: 'UNKNOWN'
            def bannerColor = pipelineStatus.toUpperCase() == 'SUCCESS' ? 'green' : 'red'

            def body = """
                <html>
                <body>
                <div style="border: 4px solid ${bannerColor}; padding: 10px;">
                <h2>${jobName} - Build ${buildNumber}</h2>
                <div style="background-color: ${bannerColor}; padding: 10px;">
                <h3 style="color: white;">Pipeline Status: ${pipelineStatus.toUpperCase()}</h3>
                </div>
                <p>Check the <a href="${BUILD_URL}">console output</a>.</p>
                </div>
                </body>
                </html>
            """

            emailext (
                subject: "${jobName} - Build ${buildNumber} - ${pipelineStatus.toUpperCase()}",
                body: body,
                to: 'cse.alisyedtanveer@gmail.com',
                from: 'jenkins@example.com',
                replyTo: 'jenkins@example.com',
                mimeType: 'text/html',
                attachmentsPattern: 'trivy-image-report.html'
            )
        }
    }
        }   
}


