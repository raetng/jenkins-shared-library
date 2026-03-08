#!/usr/bin/env groovy

/**
 * Runs Gitleaks for secret detection and archives the report.
 * Fails the build if secrets are found.
 */
def call(Map config = [:]) {
    def reportFile = config.reportFile ?: 'gitleaks-report.json'
    def configFile = config.configFile ?: ''

    echo 'Running Gitleaks secret detection scan'

    def configFlag = configFile ? "--config ${configFile}" : ''

    def exitCode = sh(
        script: """
            gitleaks detect --source . \
                ${configFlag} \
                --report-format json \
                --report-path ${reportFile} \
                --verbose
        """,
        returnStatus: true
    )

    // Archive the report regardless of outcome
    archiveArtifacts artifacts: reportFile, allowEmptyArchive: true

    if (exitCode != 0) {
        error("Gitleaks detected secrets in the repository. Review ${reportFile} for details.")
    } else {
        echo 'Gitleaks scan passed — no secrets detected.'
    }
}
