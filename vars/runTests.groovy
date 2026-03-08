#!/usr/bin/env groovy

/**
 * Runs a test command and archives the results.
 */
def call(Map config = [:]) {
    def testCommand = config.testCommand ?: error('testCommand is required')
    def resultPath  = config.resultPath  ?: 'test-results/'

    echo "Running tests: ${testCommand}"

    def exitCode = sh(script: testCommand, returnStatus: true)

    // Archive test results if the directory exists
    if (fileExists(resultPath)) {
        archiveArtifacts artifacts: "${resultPath}**/*", allowEmptyArchive: true
        junit allowEmptyResults: true, testResults: "${resultPath}**/*.xml"
    }

    if (exitCode != 0) {
        error("Tests failed with exit code ${exitCode}")
    }
}
