#!/usr/bin/env groovy

/**
 * Pushes a Docker image to Docker Hub using Jenkins credentials.
 */
def call(Map config = [:]) {
    def imageName    = config.imageName    ?: error('imageName is required')
    def tag          = config.tag          ?: error('tag is required')
    def credentialsId = config.credentialsId ?: 'dockerhub-credentials'

    echo "Pushing image: ${imageName}:${tag}"

    docker.withRegistry('https://index.docker.io/v1/', credentialsId) {
        def img = docker.image("${imageName}:${tag}")
        img.push()
        img.push('latest')
    }
}
