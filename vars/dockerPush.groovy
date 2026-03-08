#!/usr/bin/env groovy

/**
 * Pushes a Docker image to Docker Hub using Jenkins credentials.
 */
def call(Map config = [:]) {
    def imageName    = config.imageName    ?: error('imageName is required')
    def tag          = config.tag          ?: error('tag is required')
    def credentialsId = config.credentialsId ?: 'dockerhub-credentials'

    echo "Pushing image: ${imageName}:${tag}"

    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
        '''
        sh "docker push ${imageName}:${tag}"
    }
}
