pipeline {
    agent any

    environment {
        PATH = "/usr/local/bin:$PATH"
    }

    options {
        skipDefaultCheckout true
    }

    stages {
        stage('Build Maven') {
            steps {
                checkout(scm: [
                    $class: 'GitSCM',
                    branches: [[name: '*/main']],
                    doStripWorkspace: false,
                    extensions: [],
                    userRemoteConfigs: [[credentialsId: 'saravanan81java', name: 'origin']]
                ])
                sh 'mvn clean install'
            }
        }
        stage('Build docker image') {
            steps {
                script {
                    docker.build('saran81/devops-springboot-integration')
                }
            }
        }
        stage('Push image to Hub') {
            steps {
                script {
                    withCredentials([[$class: 'StringBinding', credentialsId: 'dockerhubpwd', variable: 'dockerhubpwd']]) {
                        sh 'docker login -u saran81 -p ${dockerhubpwd}'
                        sh 'docker push saran81/devops-springboot-integration'
                    }
                }
            }
        }
    }
}
