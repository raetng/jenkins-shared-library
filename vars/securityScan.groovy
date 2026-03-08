#!/usr/bin/env groovy

/**
 * Runs a Trivy vulnerability scan on a Docker image.
 * Archives the report and optionally fails the build based on severity threshold.
 *
 * Severity thresholds by environment:
 *   - Dev:          CRITICAL  (fail only on CRITICAL)
 *   - Staging/Prod: HIGH      (fail on HIGH and above)
 */
def call(Map config = [:]) {
    def imageName         = config.imageName         ?: error('imageName is required')
    def tag               = config.tag               ?: error('tag is required')
    def severityThreshold = config.severityThreshold ?: 'CRITICAL'

    def fullImage   = "${imageName}:${tag}"
    def reportFile  = "trivy-report-${imageName.replaceAll('/', '-')}-${tag}.json"
    def tableReport = "trivy-report-${imageName.replaceAll('/', '-')}-${tag}.txt"

    echo "Running Trivy scan on ${fullImage} (threshold: ${severityThreshold})"

    // Generate human-readable table report
    sh """
        trivy image --severity ${severityThreshold},CRITICAL \
            --format table \
            --output ${tableReport} \
            ${fullImage} || true
    """

    // Generate JSON report for archiving and downstream processing
    sh """
        trivy image --severity ${severityThreshold},CRITICAL \
            --format json \
            --output ${reportFile} \
            ${fullImage} || true
    """

    // Archive both reports
    archiveArtifacts artifacts: "trivy-report-*.json,trivy-report-*.txt", allowEmptyArchive: true

    // Fail the build if vulnerabilities at or above threshold are found
    def exitCode = sh(
        script: """
            trivy image --severity ${severityThreshold} \
                --exit-code 1 \
                --quiet \
                ${fullImage}
        """,
        returnStatus: true
    )

    if (exitCode != 0) {
        error("Trivy found vulnerabilities at severity ${severityThreshold} or above in ${fullImage}")
    }
}
