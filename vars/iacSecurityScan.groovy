#!/usr/bin/env groovy

/**
 * Runs tfsec static analysis on Terraform modules.
 * Archives the report and optionally fails the build based on severity threshold.
 *
 * Severity thresholds by environment:
 *   - Dev:          CRITICAL  (fail only on CRITICAL)
 *   - Staging/Prod: HIGH      (fail on HIGH and above)
 *
 * Usage:
 *   iacSecurityScan(terraformDir: 'infrastructure/terraform')
 *   iacSecurityScan(terraformDir: 'infrastructure/terraform', severityThreshold: 'HIGH', tfvarsFile: 'staging.tfvars')
 */
def call(Map config = [:]) {
    def terraformDir      = config.terraformDir      ?: 'infrastructure/terraform'
    def severityThreshold = config.severityThreshold ?: 'CRITICAL'
    def tfvarsFile        = config.tfvarsFile        ?: ''

    def tfvarsFlag = tfvarsFile ? "--tfvars-file ${tfvarsFile}" : ''
    def reportName = tfvarsFile ? "tfsec-report-${tfvarsFile.replace('.tfvars', '')}" : 'tfsec-report'

    echo "Running tfsec IaC security scan on ${terraformDir} (threshold: ${severityThreshold})"

    // Generate human-readable text report
    sh """
        tfsec ${terraformDir} ${tfvarsFlag} \
            --format text \
            --out ${reportName}.txt \
            --soft-fail || true
    """

    // Generate JSON report for archiving and downstream processing
    sh """
        tfsec ${terraformDir} ${tfvarsFlag} \
            --format json \
            --out ${reportName}.json \
            --soft-fail || true
    """

    // Archive reports
    archiveArtifacts artifacts: "tfsec-report*.json,tfsec-report*.txt", allowEmptyArchive: true

    // Map severity threshold to tfsec minimum severity
    def minSeverity = severityThreshold.toUpperCase()

    // Fail the build if findings at or above threshold exist
    def exitCode = sh(
        script: """
            tfsec ${terraformDir} ${tfvarsFlag} \
                --minimum-severity ${minSeverity} \
                --soft-fail 2>&1 | tee /dev/null
            tfsec ${terraformDir} ${tfvarsFlag} \
                --minimum-severity ${minSeverity}
        """,
        returnStatus: true
    )

    if (exitCode != 0) {
        error("tfsec found IaC security issues at severity ${severityThreshold} or above in ${terraformDir}")
    }

    echo "tfsec scan passed — no findings at ${severityThreshold} severity or above"
}
