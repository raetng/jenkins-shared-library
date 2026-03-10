#!/usr/bin/env groovy

/**
 * Verifies that Prometheus is scraping a deployed service by querying
 * the /api/v1/targets endpoint and checking for an 'up' target matching
 * the given service name and namespace.
 *
 * Parameters:
 *   serviceName - the app label of the service (e.g. 'product')
 *   namespace   - the Kubernetes namespace (e.g. 'dev', 'staging')
 *   retries     - number of attempts before failing (default: 6)
 *   retryDelay  - seconds between retries (default: 10)
 */
def call(Map config = [:]) {
    def serviceName = config.serviceName ?: error('serviceName is required')
    def namespace   = config.namespace   ?: error('namespace is required')
    def retries     = config.retries     ?: 6
    def retryDelay  = config.retryDelay  ?: 10

    echo "Verifying Prometheus is scraping '${serviceName}' in namespace '${namespace}'..."

    def minikubeIp = sh(
        script: 'minikube ip',
        returnStdout: true
    ).trim()

    def prometheusUrl = "http://${minikubeIp}:30090/api/v1/targets"

    for (int attempt = 1; attempt <= retries; attempt++) {
        echo "Attempt ${attempt}/${retries}: querying ${prometheusUrl}"

        def result = sh(
            script: """
                curl -sf '${prometheusUrl}' | \
                python3 -c "
import sys, json
data = json.load(sys.stdin)
for target in data.get('data', {}).get('activeTargets', []):
    labels = target.get('labels', {})
    if labels.get('app') == '${serviceName}' and labels.get('namespace') == '${namespace}':
        health = target.get('health', 'unknown')
        print(health)
        sys.exit(0 if health == 'up' else 1)
# No matching target found
print('not_found')
sys.exit(1)
"
            """,
            returnStatus: true
        )

        if (result == 0) {
            echo "Prometheus target '${serviceName}' in '${namespace}' is UP."
            return
        }

        if (attempt < retries) {
            echo "Target not yet up. Retrying in ${retryDelay}s..."
            sleep retryDelay
        }
    }

    unstable("Prometheus target '${serviceName}' in '${namespace}' was not found or not healthy after ${retries} attempts. Marking build as UNSTABLE.")
}
