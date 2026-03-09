#!/usr/bin/env groovy

/**
 * Deploys to Kubernetes by substituting IMAGE_TAG in manifests before applying.
 */
def call(Map config = [:]) {
    def namespace    = config.namespace    ?: error('namespace is required')
    def manifestPath = config.manifestPath ?: error('manifestPath is required')
    def imageTag     = config.imageTag     ?: error('imageTag is required')

    echo "Deploying to namespace '${namespace}' with image tag '${imageTag}'"

    def k8sRoot = '/Users/raetng/Documents/UChicago/DevOps/final/k8s'
    def srcDir  = "${k8sRoot}/${manifestPath.replaceAll('^k8s/', '').replaceAll('/\$', '')}"
    def tmpDir  = "${pwd()}/k8s-deploy-tmp"

    // Copy manifests to a temp dir so sed doesn't modify originals
    sh "rm -rf ${tmpDir} && mkdir -p ${tmpDir} && cp ${srcDir}/*.yaml ${tmpDir}/"

    // Replace the IMAGE_TAG placeholder with the actual tag
    sh "sed -i'' -e 's|IMAGE_TAG|${imageTag}|g' ${tmpDir}/*.yaml"

    // Apply the resolved manifests
    def applyOutput = sh(
        script: "kubectl apply -f ${tmpDir} -n ${namespace}",
        returnStdout: true
    ).trim()

    // Clean up temp dir
    sh "rm -rf ${tmpDir}"
    echo applyOutput

    // Extract only the deployment names from the apply output
    // Lines look like: "deployment.apps/frontend configured"
    def deployments = applyOutput.split('\n')
        .findAll { it.startsWith('deployment.apps/') }
        .collect { it.split('/')[1].split('\\s+')[0] }

    // Wait for rollout to complete
    for (dep in deployments) {
        sh "kubectl rollout status deployment/${dep} -n ${namespace} --timeout=300s"
    }
}
