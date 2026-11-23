pipeline {
    agent any

    environment {
        CI = 'true'
    }

    options {
        skipDefaultCheckout true
    }

    triggers {
        poll 'H/5 * * * *'
    }

    stages {
        stage('Build') {
            steps {
                sh 'docker run -d --name nodejs -p 3000:3000 -p 5000:5000 -v $(pwd):/app -w /app node:lts-alpine npm install'
            }
        }
        stage('Test') {
            steps {
                sh 'docker exec -it nodejs npm install'
                sh 'docker exec -it nodejs ./jenkins/scripts/test.sh'
            }
        }
        stage('Deliver for development') {
            when {
                branch 'development'
            }
            steps {
                sh 'docker exec -it nodejs ./jenkins/scripts/deliver-for-development.sh'
                input message: 'Finished using the web site? (Click "Proceed" to continue)'
                sh 'docker exec -it nodejs ./jenkins/scripts/kill.sh'
            }
        }
        stage('Deploy for production') {
            when {
                branch 'production'
            }
            steps {
                sh 'docker exec -it nodejs ./jenkins/scripts/deploy-for-production.sh'
                input message: 'Finished using the web site? (Click "Proceed" to continue)'
                sh 'docker exec -it nodejs ./jenkins/scripts/kill.sh'
            }
        }
    }
}
