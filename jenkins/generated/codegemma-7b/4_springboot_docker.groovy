pipeline {
    agent any

    environment {
        PATH = "/usr/local/bin:$PATH"
    }

    stages {
        stage('Build Maven') {
            steps {
                checkout(scmGit(
                    url: 'https://github.com/saravanan81java/devops-springboot',
                    credentialsId: 'saravanan81java',
                    branch: '*/main',
                    extensions: []
                ))
                sh 'mvn clean install'
            }
        }

        stage('Build docker image') {
            steps {
                script {
                    sh 'docker build -t saran81/devops-springboot-integration .'
                }
            }
        }

        stage('Push image to Hub') {
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhubpwd',
                            usernameVariable: 'dockerhubpwd',
                            passwordVariable: 'dockerhubpwd'
                        )
                    ]) {
                        sh 'docker login -u saran81 -p ${dockerhubpwd}'
                        sh 'docker push saran81/devops-springboot-integration'
                    }
                }
            }
        }
    }
}
