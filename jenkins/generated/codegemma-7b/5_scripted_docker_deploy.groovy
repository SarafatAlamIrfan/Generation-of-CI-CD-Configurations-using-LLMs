pipeline {
    agent any

    stages {
        stage('Clone Repo') {
            steps {
                git url: 'https://github.com/vdharmaraj/PGDO_Proj3.git', branch: 'main'
                withEnv {
                    mvnHome = tool 'maven-3.5.2'
                }
            }
        }

        stage('Build Project') {
            steps {
                sh "'${mvnHome}/bin/mvn' clean install"
            }
        }

        stage('Build Docker Image with new code') {
            steps {
                docker.withRegistry('', 'dockerHub') {
                    dockerImage = docker.build("${dockerhubaccountid}/${application}:${env.BUILD_NUMBER}")
                }
            }
        }

        stage('Push Image to Remote Repo') {
            steps {
                echo "Docker Image Tag Name ---> ${dockerImageTag}"
                docker.withRegistry('', 'dockerHub') {
                    docker.push(image: "${env.BUILD_NUMBER}", tag: "latest")
                }
            }
        }

        stage('Remove running container with old code') {
            steps {
                sh "docker rm -f \$(docker ps -a -f name=devopsexample -q) || true"
            }
        }

        stage('Deploy Docker Image with new changes') {
            steps {
                sh "docker run --name devopsexample -d -p 2222:2222 ${dockerhubaccountid}/${application}:${env.BUILD_NUMBER}"
            }
        }

        stage('Remove old images') {
            steps {
                sh "docker rmi ${dockerhubaccountid}/${application}:latest -f"
            }
        }
    }
}
