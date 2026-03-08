#!/usr/bin/env groovy

/**
 * Deploys to Kubernetes by applying manifests and updating the image tag.
 */
def call(Map config = [:]) {
    def namespace    = config.namespace    ?: error('namespace is required')
    def manifestPath = config.manifestPath ?: error('manifestPath is required')
    def imageTag     = config.imageTag     ?: error('imageTag is required')

    echo "Deploying to namespace '${namespace}' with image tag '${imageTag}'"

    // Apply the manifests and capture which resources were created/configured
    def applyOutput = sh(
        script: "kubectl apply -f ${manifestPath} -n ${namespace}",
        returnStdout: true
    ).trim()
    echo applyOutput

    // Extract only the deployment names from the apply output
    // Lines look like: "deployment.apps/frontend-deployment configured"
    def deployments = applyOutput.split('\n')
        .findAll { it.startsWith('deployment.apps/') }
        .collect { it.split('/')[1].split('\\s+')[0] }

    if (deployments.isEmpty()) {
        echo "WARNING: No deployments found in ${manifestPath}; skipping image update."
        return
    }

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
