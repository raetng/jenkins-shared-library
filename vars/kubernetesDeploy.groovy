#!/usr/bin/env groovy

/**
 * Deploys to Kubernetes by applying manifests and updating the image tag.
 */
def call(Map config = [:]) {
    def namespace    = config.namespace    ?: error('namespace is required')
    def manifestPath = config.manifestPath ?: error('manifestPath is required')
    def imageTag     = config.imageTag     ?: error('imageTag is required')

    echo "Deploying to namespace '${namespace}' with image tag '${imageTag}'"

    // Apply the manifests
    sh "kubectl apply -f ${manifestPath} -n ${namespace}"

    // Update the image tag on all deployments found in the manifest directory
    def deployments = sh(
        script: "kubectl get deployments -n ${namespace} -o jsonpath='{.items[*].metadata.name}'",
        returnStdout: true
    ).trim().split('\\s+')

    for (dep in deployments) {
        def containers = sh(
            script: "kubectl get deployment ${dep} -n ${namespace} -o jsonpath='{.spec.template.spec.containers[*].name}'",
            returnStdout: true
        ).trim().split('\\s+')

        for (container in containers) {
            def currentImage = sh(
                script: "kubectl get deployment ${dep} -n ${namespace} -o jsonpath='{.spec.template.spec.containers[?(@.name==\"${container}\")].image}'",
                returnStdout: true
            ).trim()

            def baseImage = currentImage.contains(':') ? currentImage.split(':')[0] : currentImage
            sh "kubectl set image deployment/${dep} ${container}=${baseImage}:${imageTag} -n ${namespace}"
        }
    }

    // Wait for rollout to complete
    for (dep in deployments) {
        sh "kubectl rollout status deployment/${dep} -n ${namespace} --timeout=300s"
    }
}
