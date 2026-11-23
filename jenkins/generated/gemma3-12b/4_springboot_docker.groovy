pipeline {
    agent any
    tools {
        maven 'Default'
    }
    environment {
        PATH = "/usr/local/bin:$PATH"
    }
    stages {
        stage('Build Maven') {
            steps {
                checkout scmGit(
                    credentialsId: 'saravanan81java',
                    url: 'https://github.com/saravanan81java/devops-springboot',
                    branchSpecifier: [[name: 'main']]
                )
                sh 'mvn clean install'
            }
        }
        stage('Build docker image') {
            steps {
                script {
                    docker.build("-t saran81/devops-springboot-integration .")
                }
            }
        }
        stage('Push image to Hub') {
            steps {
                withCredentials([string(credentialsId: 'dockerhubpwd', variable: 'dockerhubpwd')]) {
                    script {
                        docker.withRegistry('https://registry.hub.docker.com', 'saran81') {
                            sh "docker login -u saran81 -p ${dockerhubpwd}"
                            sh "docker push saran81/devops-springboot-integration"
                        }
                    }
                }
            }
        }
    }
}
