/**
 * Script Name: disk-usage-cleanup.groovy
 * Target: Jenkins Script Console (Manage Jenkins -> Script Console)
 * Description: Targeted cleanup of the top disk consumers identified by disk-usage-report.groovy.
 *              Covers: stale Git caches, job build history, job workspaces, config-history,
 *              Synopsys Detect downloads, and npm/JDK caches.
 * Safe for: JaaS / Cloud environments without direct SSH/filesystem access.
 *
 * USAGE:
 *   1. Set DRY_RUN = true and run — review what would be deleted.
 *   2. Set DRY_RUN = false and run — perform the actual deletion.
 */

import jenkins.model.Jenkins
import java.nio.file.Files
import java.nio.file.Path
import java.util.Calendar

// -----------------------------------------------------------------------
// CONFIGURATION
// -----------------------------------------------------------------------

// Set to false to perform actual deletion
boolean DRY_RUN = true

// Remove Git cache dirs (caches/git-*) older than this many days
int GIT_CACHE_MAX_AGE_DAYS = 30

// Remove per-job build records older than this many days (build history inside jobs/**/builds/)
int BUILD_HISTORY_MAX_AGE_DAYS = 30

// Remove job workspace dirs (jobs/**/workspace*) not modified in this many days
int WORKSPACE_MAX_AGE_DAYS = 14

// Remove config-history entries (all sub-trees, including credentials) older than this many days
// Note: the companion script cleanup-jenkins-config-history.groovy targets only the root level;
// this one covers all sub-trees (e.g. config-history/credentials).
int CONFIG_HISTORY_MAX_AGE_DAYS = 14

// Remove Synopsys Detect / BlackDuck downloaded JARs older than this many days
int DETECT_DOWNLOAD_MAX_AGE_DAYS = 30

// Remove npm cache entries older than this many days
int NPM_CACHE_MAX_AGE_DAYS = 30

// -----------------------------------------------------------------------

File home = Jenkins.instance.rootDir
String dryRunLabel = DRY_RUN ? "[DRY RUN] " : ""

long totalReclaimed = 0
int totalDeleted = 0

def formatSize = { long bytes ->
    if (bytes >= 1_073_741_824) return String.format("%.2f GB", bytes / 1_073_741_824)
    if (bytes >= 1_048_576)     return String.format("%.2f MB", bytes / 1_048_576)
    if (bytes >= 1_024)         return String.format("%.2f KB", bytes / 1_024)
    return "${bytes} B"
}

def dirSize
dirSize = { File dir ->
    long total = 0
    try {
        dir.eachFileRecurse { f -> if (f.isFile()) total += f.length() }
    } catch (ignored) {}
    total
}

def cutoff = { int days ->
    Calendar c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -days)
    c.getTimeInMillis()
}

def deleteDir = { File dir, String reason ->
    long size = dirSize(dir)
    if (DRY_RUN) {
        println "${dryRunLabel}Would delete: ${dir.absolutePath}  (${formatSize(size)})  [${reason}]"
    } else {
        boolean ok = dir.deleteDir()
        println "${ok ? 'Deleted' : 'FAILED'}:  ${dir.absolutePath}  (${formatSize(size)})  [${reason}]"
        if (ok) {
            totalReclaimed += size
            totalDeleted++
        }
    }
    if (DRY_RUN) { totalReclaimed += size; totalDeleted++ }
}

def deleteFile = { File f, String reason ->
    long size = f.length()
    if (DRY_RUN) {
        println "${dryRunLabel}Would delete: ${f.absolutePath}  (${formatSize(size)})  [${reason}]"
    } else {
        boolean ok = f.delete()
        println "${ok ? 'Deleted' : 'FAILED'}:  ${f.absolutePath}  (${formatSize(size)})  [${reason}]"
        if (ok) { totalReclaimed += size; totalDeleted++ }
    }
    if (DRY_RUN) { totalReclaimed += size; totalDeleted++ }
}

// -----------------------------------------------------------------------
// 1. Stale Git caches  (caches/git-*)
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "1. Stale Git caches  (caches/git-*)  — older than ${GIT_CACHE_MAX_AGE_DAYS} days"
println "=" * 70

File cachesDir = new File(home, "caches")
if (cachesDir.exists()) {
    long cutoffTs = cutoff(GIT_CACHE_MAX_AGE_DAYS)
    cachesDir.eachDir { dir ->
        if (dir.name.startsWith("git-") && dir.lastModified() < cutoffTs) {
            deleteDir(dir, "stale git cache")
        }
    }
} else {
    println "Directory not found: ${cachesDir.absolutePath}"
}

// -----------------------------------------------------------------------
// 2. Old build records  (jobs/**/builds/<number>)
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "2. Old build records  — older than ${BUILD_HISTORY_MAX_AGE_DAYS} days"
println "=" * 70

long buildCutoffTs = cutoff(BUILD_HISTORY_MAX_AGE_DAYS)
def buildNumberPattern = ~/^\d+$/

Files.walk(new File(home, "jobs").toPath()).forEach { Path p ->
    File f = p.toFile()
    if (f.isDirectory() && f.name == "builds") {
        f.eachDir { buildDir ->
            if (buildDir.name =~ buildNumberPattern && buildDir.lastModified() < buildCutoffTs) {
                deleteDir(buildDir, "old build record")
            }
        }
    }
}

// -----------------------------------------------------------------------
// 3. Stale job workspaces  (jobs/**/workspace*)
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "3. Stale job workspaces  — not modified in ${WORKSPACE_MAX_AGE_DAYS} days"
println "=" * 70

long wsCutoffTs = cutoff(WORKSPACE_MAX_AGE_DAYS)

Files.walk(new File(home, "jobs").toPath()).forEach { Path p ->
    File f = p.toFile()
    if (f.isDirectory() && f.name.startsWith("workspace") && f.lastModified() < wsCutoffTs) {
        deleteDir(f, "stale workspace")
    }
}

// -----------------------------------------------------------------------
// 4. config-history all sub-trees  (including credentials)
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "4. config-history entries (all sub-trees)  — older than ${CONFIG_HISTORY_MAX_AGE_DAYS} days"
println "=" * 70

File configHistoryRoot = new File(home, "config-history")
long configCutoffTs = cutoff(CONFIG_HISTORY_MAX_AGE_DAYS)
def timestampPattern = ~/^\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}$/

if (configHistoryRoot.exists()) {
    List<File> histDirs = []
    Files.walk(configHistoryRoot.toPath()).forEach { Path p ->
        File f = p.toFile()
        if (f.isDirectory() && (f.name =~ timestampPattern) && f.lastModified() < configCutoffTs) {
            histDirs << f
        }
    }
    println "Found ${histDirs.size()} timestamp entries to remove."
    histDirs.each { deleteDir(it, "old config-history entry") }
} else {
    println "Directory not found: ${configHistoryRoot.absolutePath}"
}

// -----------------------------------------------------------------------
// 5. Synopsys Detect / BlackDuck downloaded JARs
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "5. Synopsys Detect downloads  — older than ${DETECT_DOWNLOAD_MAX_AGE_DAYS} days"
println "=" * 70

long detectCutoffTs = cutoff(DETECT_DOWNLOAD_MAX_AGE_DAYS)

[new File(home, "detect/download"), new File(home, "synopsys-detect/download")].each { dlDir ->
    if (dlDir.exists()) {
        dlDir.eachFile { f ->
            if (f.lastModified() < detectCutoffTs) {
                f.isDirectory() ? deleteDir(f, "old detect download") : deleteFile(f, "old detect download")
            }
        }
    } else {
        println "Directory not found: ${dlDir.absolutePath}"
    }
}

// -----------------------------------------------------------------------
// 6. npm cache  (.npm/_cacache)
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "6. npm cache  (.npm/_cacache)  — older than ${NPM_CACHE_MAX_AGE_DAYS} days"
println "=" * 70

long npmCutoffTs = cutoff(NPM_CACHE_MAX_AGE_DAYS)
File npmCache = new File(home, ".npm/_cacache")

if (npmCache.exists()) {
    npmCache.eachDir { bucket ->
        bucket.eachFile { f ->
            if (f.lastModified() < npmCutoffTs) {
                deleteFile(f, "old npm cache entry")
            }
        }
    }
} else {
    println "Directory not found: ${npmCache.absolutePath}"
}

// -----------------------------------------------------------------------
// SUMMARY
// -----------------------------------------------------------------------
println "\n${"=" * 70}"
println "${DRY_RUN ? 'DRY RUN SUMMARY' : 'CLEANUP COMPLETE'}"
println "=" * 70
println "${dryRunLabel}Items targeted : ${totalDeleted}"
println "${dryRunLabel}Space ${DRY_RUN ? 'reclaimable' : 'reclaimed'} : ${formatSize(totalReclaimed)}"
if (DRY_RUN) {
    println "\nSet DRY_RUN = false and re-run to apply changes."
}
