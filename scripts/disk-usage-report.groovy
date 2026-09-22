/**
 * Script Name: disk-usage-report.groovy
 * Target: Jenkins Script Console (Manage Jenkins -> Script Console)
 * Description: Reports top N directories under JENKINS_HOME ranked by disk usage.
 *              Run this first to identify what is consuming the most space,
 *              then use the companion cleanup script with the results.
 * Safe for: JaaS / Cloud environments without direct SSH/filesystem access.
 */

import jenkins.model.Jenkins
import java.nio.file.Files
import java.nio.file.Path

// How many levels deep to report (1 = direct children of JENKINS_HOME, 2 = one level deeper, etc.)
int scanDepth = 2

// Number of top directories to display per level
int topN = 20

// -----------------------------------------------------------------------

File jenkinsHome = Jenkins.instance.rootDir
println "Jenkins Home: ${jenkinsHome.absolutePath}"
println "Scanning top-${topN} directories at depth 1..${scanDepth} by size...\n"

def formatSize = { long bytes ->
    if (bytes >= 1_073_741_824) return String.format("%.2f GB", bytes / 1_073_741_824)
    if (bytes >= 1_048_576)     return String.format("%.2f MB", bytes / 1_048_576)
    if (bytes >= 1_024)         return String.format("%.2f KB", bytes / 1_024)
    return "${bytes} B"
}

// Recursively sum all file sizes under a directory
def dirSize
dirSize = { File dir ->
    long total = 0
    dir.eachFile { f ->
        total += f.isDirectory() ? dirSize(f) : f.length()
    }
    total
}

// Collect all directories at each requested depth
def scanLevel = { File root, int currentDepth, int maxDepth ->
    Map<File, Long> sizes = [:]
    root.eachFile { f ->
        if (f.isDirectory()) {
            long size = dirSize(f)
            sizes[f] = size
        }
    }
    sizes
}

for (int depth = 1; depth <= scanDepth; depth++) {
    println "=" * 70
    println "DEPTH ${depth} — Top ${topN} directories"
    println "=" * 70

    // Collect all dirs at this depth
    List<File> dirsAtDepth = []
    Files.walk(jenkinsHome.toPath(), depth).forEach { Path p ->
        if (p.toFile().isDirectory() && p != jenkinsHome.toPath()) {
            int pathDepth = jenkinsHome.toPath().relativize(p).nameCount
            if (pathDepth == depth) {
                dirsAtDepth << p.toFile()
            }
        }
    }

    // Sort by size descending
    def ranked = dirsAtDepth
        .collect { dir -> [dir: dir, size: dirSize(dir)] }
        .sort { -it.size }
        .take(topN)

    ranked.each { entry ->
        String relPath = jenkinsHome.toPath().relativize(entry.dir.toPath()).toString()
        println String.format("  %-60s %s", relPath, formatSize(entry.size))
    }
    println ""
}

println "=" * 70
println "Scan complete. Share these results to generate a targeted cleanup script."
println "=" * 70
