pipeline {
    agent { label '!windows' }
    options { skipStagesAfterUnstable() }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    publishJUnit([
                        allowEmptyResults: true,
                        reportDir: 'target/surefire-reports/',
                        retainDataOnFailure: false
                    ])
                }
            }
        }
        stage('Deliver') {
            steps {
                sh './jenkins/scripts/deliver.sh'
            }
        }
    }
}
