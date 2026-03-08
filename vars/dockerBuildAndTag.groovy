#!/usr/bin/env groovy

/**
 * Builds a Docker image and tags it using the project's tagging strategy:
 *   - develop branch:        dev-<SHA>
 *   - release/* branches:    rc-<SHA>
 *   - main branch (prod):    v<VERSION> (read from VERSION file)
 */
def call(Map config = [:]) {
    def imageName     = config.imageName     ?: error('imageName is required')
    def tag           = config.tag           ?: resolveTag()
    def dockerfilePath = config.dockerfilePath ?: '.'

    echo "Building image: ${imageName}:${tag} from ${dockerfilePath}"

    sh "docker build -t ${imageName}:${tag} ${dockerfilePath}"

    return tag
}

/**
 * Determines the image tag based on the current branch.
 */
private String resolveTag() {
    def gitCommitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    def branch = env.BRANCH_NAME ?: sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()

    if (branch == 'main' || branch == 'master') {
        def version = readVersionFile()
        return "v${version}"
    } else if (branch.startsWith('release/')) {
        return "rc-${gitCommitShort}"
    } else {
        // develop and all other branches
        return "dev-${gitCommitShort}"
    }
}

/**
 * Reads the VERSION file from the repo root.
 */
private String readVersionFile() {
    if (!fileExists('VERSION')) {
        error('VERSION file not found in repo root. Required for production tagging.')
    }
    return readFile('VERSION').trim()
}
