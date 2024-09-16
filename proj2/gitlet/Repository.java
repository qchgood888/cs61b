package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *
 *  @author Jeffrey
 */
public class Repository {

    /**
     * Default branch name.
     */
    private static final String DEFAULT_BRANCH_NAME = "master";

    /**
     * HEAD ref prefix.
     */
    private static final String HEAD_BRANCH_REF_PREFIX = "ref: refs/heads/";

    /**
     * The current working directory.
     */
    private static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * The .gitlet directory.
     */
    private static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The index file.
     */
    public static final File INDEX = join(GITLET_DIR, "index");

    /**
     * The objects directory.
     */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");

    /**
     * The HEAD file.
     */
    private static final File HEAD = join(GITLET_DIR, "HEAD");

    /**
     * The refs directory.
     */
    private static final File REFS_DIR = join(GITLET_DIR, "refs");

    /**
     * The heads directory.
     */
    private static final File BRANCHES_HEADS_DIR = join(REFS_DIR, "heads");

    /**
     * Files in the current working directory.
     */
    private static final Lazy<File[]> currentFiles = lazy(() -> CWD.listFiles(File::isFile));

    /**
     * The current branch name.
     */
    private final Lazy<String> currentBranch = lazy(() -> {
        String HEADFileContent = readContentsAsString(HEAD);
        return HEADFileContent.replace(HEAD_BRANCH_REF_PREFIX, "");
    });

    /**
     * The commit that HEAD points to.
     */
    private final Lazy<Commit> HEADCommit = lazy(() -> getBranchHeadCommit(currentBranch.get()));

    /**
     * The staging area instance. Initialized in the constructor.
     */
    private final Lazy<StagingArea> stagingArea = lazy(() -> {
        StagingArea stagingArea = INDEX.exists() ? StagingArea.fromFile() : new StagingArea();
        stagingArea.setTracked(HEADCommit.get().getTracked());
        return stagingArea;
    });

    /**
     * Initialize a repository at the current working directory.
     *
     * <pre>
     * .gitlet
     * |- HEAD
     * |- objects
     * |- refs
     *     |- heads
     * </pre>
     */
    public static void init() {
        if (GITLET_DIR.exists()) {
            exit("A Gitlet version-control system already exists in the current directory.");
        }
        mkdir(GITLET_DIR);
        mkdir(REFS_DIR);
        mkdir(BRANCHES_HEADS_DIR);
        mkdir(OBJECTS_DIR);
        setCurrentBranch(DEFAULT_BRANCH_NAME);
        createInitialCommit();
    }

    /**
     * Set current branch.
     *
     * @param branchName Name of the branch
     */
    private static void setCurrentBranch(String branchName) {
        writeContents(HEAD, HEAD_BRANCH_REF_PREFIX + branchName);
    }

    /**
     * Create an initial commit.
     */
    private static void createInitialCommit() {
        Commit initialCommit = new Commit();
        initialCommit.save();
        setBranchHeadCommit(DEFAULT_BRANCH_NAME, initialCommit.getId());
    }

    /**
     * Set branch head.
     *
     * @param branchName Name of the branch
     * @param commitId Commit SHA1 id
     */
    private static void setBranchHeadCommit(String branchName, String commitId) {
        File branchHeadFile = getBranchHeadFile(branchName);
        setBranchHeadCommit(branchHeadFile, commitId);
    }

    /**
     * Get branch head ref file in refs/heads folder.
     *
     * @param branchName Name of the branch
     * @return File instance
     */
    private static File getBranchHeadFile(String branchName) {
        return join(BRANCHES_HEADS_DIR, branchName);
    }

    /**
     * Set branch head.
     *
     * @param branchHeadFile File instance
     * @param commitId Commit SHA1 id
     */
    private static void setBranchHeadCommit(File branchHeadFile, String commitId) {
        writeContents(branchHeadFile, commitId);
    }

    /**
     * Exit if the repository at the current working directory is not initialized.
     */
    public static void checkWorkingDir() {
        if (!(GITLET_DIR.exists() && GITLET_DIR.isDirectory())) {
            exit("Not in an initialized Gitlet directory.");
        }
    }

    /**
     * Add file to the staging area.
     */
    public void add(String fileName) {
        File file = getFileFromCWD(fileName);
        if (!file.exists()) {
            exit("File does not exist.");
        }
        if (stagingArea.get().add(file)) {
            stagingArea.get().save();
        }
    }

    /**
     * Get a File instance from CWD by the name.
     *
     * @param fileName Name of the file
     *
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        return Paths.get(fileName).isAbsolute() ? new File(fileName) : join(CWD, fileName);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

    /**
     * Get head commit of the branch.
     *
     * @param branchHeadFile File instance
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(File branchHeadFile) {
        String HEADCommitId = readContentsAsString(branchHeadFile);
        return Commit.fromFile(HEADCommitId);
    }

    /**
     * Perform a commit with message.
     *
     * @param msg Commit message
     */
    public void commit(String msg) {
        commit(msg, null);
    }

    /**
     * Perform a commit with message and two parents.
     *
     * @param msg          Commit message
     * @param secondParent Second parent Commit SHA1 id
     */
    private void commit(String msg, String secondParent) {
        if (stagingArea.get().isClean()) {
            exit("No changes added to the commit.");
        }
        Map<String, String> newTrackedFilesMap = stagingArea.get().commit();
        stagingArea.get().save();
        List<String> parents = new ArrayList<>();
        parents.add(HEADCommit.get().getId());
        if (secondParent != null) {
            parents.add(secondParent);
        }
        Commit newCommit = new Commit(msg, parents, newTrackedFilesMap);
        newCommit.save();
        setBranchHeadCommit(currentBranch.get(), newCommit.getId());
    }

    /**
     * Remove file.
     *
     * @param fileName fileName Name of the file
     */
    public void remove(String fileName) {
        File file = getFileFromCWD(fileName);
        if (stagingArea.get().remove(file)) {
            stagingArea.get().save();
        } else {
            exit("No reason to remove the file.");
        }
    }

    /**
     * print log of the current branch.
     */
    public void log() {
        StringBuilder logBuilder = new StringBuilder();
        Commit curCommit = HEADCommit.get();
        while (true) {
            logBuilder.append(curCommit.getLog()).append("\n");
            List<String> parentCommitIds = curCommit.getParents();
            if (parentCommitIds.isEmpty()) {
                break;
            }
            String firstParentCommitId = parentCommitIds.get(0);
            curCommit = Commit.fromFile(firstParentCommitId);
        }
        System.out.println(logBuilder);
    }

    /**
     * Print all commits logs ever made.
     */
    public static void globalLog() {
        StringBuilder logBuilder = new StringBuilder();
        // As the project goes, the runtime should be O(N) where N is the number of commits ever made.
        // But here I choose to log the commits in the order of created date, which has a runtime of O(NlogN).
        forEachCommit(commit -> logBuilder.append(commit.getLog()).append("\n"));
        System.out.println(logBuilder);
    }

    /**
     * Iterate all commits in the order of created date.
     * and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    private static void forEachCommitInOrder(Consumer<Commit> cb) {
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsPriorityQueue = new PriorityQueue<>(commitComparator);
        forEachCommit(cb, commitsPriorityQueue);
    }

    /**
     * Iterate all commits and execute callback function on each of them.
     *
     * @param cb Function that accepts Commit as a single argument
     */
    private static void forEachCommit(Consumer<Commit> cb) {
        Queue<Commit> commitsQueue = new ArrayDeque<>();
        forEachCommit(cb, commitsQueue);
    }

    /**
     * Helper method to iterate all commits.
     *
     * @param cb                 Callback function executed on the current commit
     * @param queueToHoldCommits New Queue instance to hold the commits while iterating
     */
    @SuppressWarnings("ConstantConditions")
    private static void forEachCommit(Consumer<Commit> cb, Queue<Commit> queueToHoldCommits) {
        Set<String> checkedCommitIds = new HashSet<>();

        File[] branchHeadFiles = BRANCHES_HEADS_DIR.listFiles();
        Arrays.sort(branchHeadFiles, Comparator.comparing(File::getName));

        for (File branchHeadFile : branchHeadFiles) {
            String branchHeadCommitId = readContentsAsString(branchHeadFile);
            if (checkedCommitIds.contains(branchHeadCommitId)) {
                continue;
            }
            checkedCommitIds.add(branchHeadCommitId);
            Commit branchHeadCommit = Commit.fromFile(branchHeadCommitId);
            queueToHoldCommits.add(branchHeadCommit);
        }

        while (true) {
            Commit nextCommit = queueToHoldCommits.poll();
            cb.accept(nextCommit);
            List<String> parentCommitIds = nextCommit.getParents();
            if (parentCommitIds.isEmpty()) {
                break;
            }
            for (String parentCommitId : parentCommitIds) {
                if (checkedCommitIds.contains(parentCommitId)) {
                    continue;
                }
                checkedCommitIds.add(parentCommitId);
                Commit parentCommit = Commit.fromFile(parentCommitId);
                queueToHoldCommits.add(parentCommit);
            }
        }
    }

    /**
     * Print all commits that have the exact message.
     *
     * @param msg Content of the message
     */
    public static void find(String msg) {
        StringBuilder resultBuilder = new StringBuilder();
        forEachCommit(commit -> {
            if (commit.getMessage().equals(msg)) {
                resultBuilder.append(commit.getId()).append("\n");
            }
        });
        if (resultBuilder.length() == 0) {
            exit("Found no commit with that message.");
        }
        System.out.println(resultBuilder);
    }

    /**
     * Print the status
     */
    @SuppressWarnings("ConstantConditions")
    public void status() {
        StringBuilder statusBuilder = new StringBuilder();

        // branches
        statusBuilder.append("=== Branches ===").append("\n");
        statusBuilder.append("*").append(currentBranch.get()).append("\n");
        String[] branchNames = BRANCHES_HEADS_DIR.list((dir, name) -> !name.equals(currentBranch.get()));
        Arrays.sort(branchNames);
        for (String branchName : branchNames) {
            statusBuilder.append(branchName).append("\n");
        }
        statusBuilder.append("\n");

        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilePathsSet = stagingArea.get().getRemoved();

        // staged files
        statusBuilder.append("=== Staged Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, addedFilesMap.keySet());
        statusBuilder.append("\n");

        // removed files
        statusBuilder.append("=== Removed Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, removedFilePathsSet);
        statusBuilder.append("\n");

        // modifications not staged for commit
        statusBuilder.append("=== Modifications Not Staged For Commit ===").append("\n");
        List<String> modifiedNotStageFilePaths = new ArrayList<>();
        Set<String> deletedNotStageFilePaths = new HashSet<>();

        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();

        trackedFilesMap.putAll(addedFilesMap);
        for (String filePath : removedFilePathsSet) {
            trackedFilesMap.remove(filePath);
        }

        for (Map.Entry<String, String> entry : trackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            String blobId = entry.getValue();

            String currentFileBlobId = currentFilesMap.get(filePath);

            if (currentFileBlobId != null) {
                if (!currentFileBlobId.equals(blobId)) {
                    // 1. Tracked in the current commit, changed in the working directory, but not staged; or
                    // 2. Staged for addition, but with different contents than in the working directory.
                    modifiedNotStageFilePaths.add(filePath);
                }
                currentFilesMap.remove(filePath);
            } else {
                // 3. Staged for addition, but deleted in the working directory; or
                // 4. Not staged for removal, but tracked in the current commit and deleted from the working directory.
                modifiedNotStageFilePaths.add(filePath);
                deletedNotStageFilePaths.add(filePath);
            }
        }

        modifiedNotStageFilePaths.sort(String::compareTo);

        for (String filePath : modifiedNotStageFilePaths) {
            String fileName = Paths.get(filePath).getFileName().toString();
            statusBuilder.append(fileName);
            if (deletedNotStageFilePaths.contains(filePath)) {
                statusBuilder.append(" ").append("(deleted)");
            } else {
                statusBuilder.append(" ").append("(modified)");
            }
            statusBuilder.append("\n");
        }
        statusBuilder.append("\n");

        // untracked files
        statusBuilder.append("=== Untracked Files ===").append("\n");
        appendFileNamesInOrder(statusBuilder, currentFilesMap.keySet());
        statusBuilder.append("\n");

        System.out.println(statusBuilder);
    }

    /**
     * Append lines of file name in order from files paths Set to StringBuilder.
     *
     * @param stringBuilder       StringBuilder instance
     * @param filePathsCollection Collection of file paths
     */
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, Collection<String> filePathsCollection) {
        List<String> filePathsList = new ArrayList<>(filePathsCollection);
        appendFileNamesInOrder(stringBuilder, filePathsList);
    }

    /**
     * Append lines of file name in order from files paths Set to StringBuilder.
     *
     * @param stringBuilder StringBuilder instance
     * @param filePathsList List of file paths
     */
    private static void appendFileNamesInOrder(StringBuilder stringBuilder, List<String> filePathsList) {
        filePathsList.sort(String::compareTo);
        for (String filePath : filePathsList) {
            String fileName = Paths.get(filePath).getFileName().toString();
            stringBuilder.append(fileName).append("\n");
        }
    }

    /**
     * Get a Map of file paths and their SHA1 id from CWD.
     *
     * @return Map with file path as key and SHA1 id as value
     */
    private static Map<String, String> getCurrentFilesMap() {
        Map<String, String> filesMap = new HashMap<>();
        for (File file : currentFiles.get()) {
            String filePath = file.getPath();
            String blobId = Blob.generatedId(file);
            filesMap.put(filePath, blobId);
        }
        return filesMap;
    }

    /**
     * Checkout file from HEAD Commit.
     *
     * @param fileName fileName of the file
     */
    public void checkout(String fileName) {
        String filePath = getFileFromCWD(fileName).getPath();
        if (!HEADCommit.get().restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * Checkout file from specific commit id.
     *
     * @param commitId Commit SHA1 id
     * @param fileName Name of the file
     */
    public void checkout(String commitId, String fileName) {
        commitId = getActualCommitId(commitId);
        String filePath = getFileFromCWD(fileName).getPath();
        if (!Commit.fromFile(commitId).restoreTracked(filePath)) {
            exit("File does not exist in that commit.");
        }
    }

    /**
     * Get the whole commit id. Exit with message if it does not exist.
     *
     * @param commitId Abbreviate or Whole commit SHA1 id
     * @return whole commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static String getActualCommitId(String commitId) {
        if (commitId.length() < UID_LENGTH) {
            if (commitId.length() < 4) {
                exit("Commit id should contain at least 4 characters.");
            }
            String objectDirName = getObjectDirName(commitId);
            File objectDir = join(OBJECTS_DIR, objectDirName);
            if (!objectDir.exists()) {
                exit("No commit with that id exists.");
            }

            boolean isFound = false;
            String objectFileNamePrefix = getObjectFileName(commitId);

            for (File objectFile : objectDir.listFiles()) {
                String objectFileName = objectFile.getName();
                if (objectFileName.startsWith(objectFileNamePrefix) && isFileInstanceOf(objectFile, Commit.class)) {
                    if (isFound) {
                        exit("More than 1 commit has the same id prefix");
                    }
                    commitId = objectDirName + objectFileName;
                    isFound = true;
                }
            }
            if (!isFound) {
                exit("No commit with that id exists.");
            }
        } else {
            if (!getObjectFile(commitId).exists()) {
                exit("No commit with that id exists.");
            }
        }
        return commitId;
    }

    /**
     * Checkout to branch.
     *
     * @param targetBranchName Name of the target branch
     */
    public void checkoutBranch(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("No such branch exists.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("No need to checkout the current branch.");
        }
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        checkUntracked(targetBranchHeadCommit);
        checkoutCommit(targetBranchHeadCommit);
        setCurrentBranch(targetBranchName);
    }

    /**
     * Exit with message if target would overwrite the untracked files.
     *
     * @param targetCommit Commit SHA1 id
     */
    private void checkUntracked(Commit targetCommit) {
        Map<String, String> currentFilesMap = getCurrentFilesMap();
        Map<String, String> trackedFilesMap = HEADCommit.get().getTracked();
        Map<String, String> addedFilesMap = stagingArea.get().getAdded();
        Set<String> removedFilesMap = stagingArea.get().getRemoved();

        List<String> untrackedFilePaths = new ArrayList<>();

        for (String filePath : currentFilesMap.keySet()) {
            if (trackedFilesMap.containsKey(filePath)) {
                if (removedFilesMap.contains(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            } else {
                if (!addedFilesMap.containsKey(filePath)) {
                    untrackedFilePaths.add(filePath);
                }
            }
        }

        Map<String, String> targetCommitTrackedFilesMap = targetCommit.getTracked();

        for (String filePath : untrackedFilePaths) {
            String blobId = currentFilesMap.get(filePath);
            String targetBlobId = targetCommitTrackedFilesMap.get(filePath);
            if (!blobId.equals(targetBlobId)) {
                exit("There is an untracked file in the way; delete it, or add and commit it first.");
            }
        }
    }

    /**
     * Checkout to specific commit.
     *
     * @param targetCommit Commit instance
     */
    private void checkoutCommit(Commit targetCommit) {
        stagingArea.get().clear();
        stagingArea.get().save();
        for (File file: currentFiles.get()) {
            rm(file);
        }
        targetCommit.restoreAllTracked();
    }

    /**
     * Create a new branch.
     *
     * @param newBranchName Name of the new branch
     */
    public void branch(String newBranchName) {
        File newBranchHeadFile = getBranchHeadFile(newBranchName);
        if (newBranchHeadFile.exists()) {
            exit("A branch with that name already exists.");
        }
        setBranchHeadCommit(newBranchHeadFile, HEADCommit.get().getId());
    }

    /**
     * Delete the branch.
     *
     * @param targetBranchName Name of the new branch
     */
    public void rmBranch(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot remove the current branch.");
        }
        rm(targetBranchHeadFile);
    }

    /**
     * Reset to commit with the id.
     *
     * @param commitId Commit SHA1 id
     */
    public void reset(String commitId) {
        commitId = getActualCommitId(commitId);
        Commit targetCommit = Commit.fromFile(commitId);
        checkUntracked(targetCommit);
        checkoutCommit(targetCommit);
        setBranchHeadCommit(currentBranch.get(), commitId);
    }

    /**
     * Merge branch.
     * @param targetBranchName Name of the target branch
     */
    public void merge(String targetBranchName) {
        File targetBranchHeadFile = getBranchHeadFile(targetBranchName);
        if (!targetBranchHeadFile.exists()) {
            exit("A branch with that name does not exist.");
        }
        if (targetBranchName.equals(currentBranch.get())) {
            exit("Cannot merge a branch with itself");
        }
        if (!stagingArea.get().isClean()) {
            exit("You have uncommitted changes");
        }
        Commit targetBranchHeadCommit = getBranchHeadCommit(targetBranchHeadFile);
        checkUntracked(targetBranchHeadCommit);

        Commit lcaCommit = getLatestCommonAncestorCommit(HEADCommit.get(), targetBranchHeadCommit);
        String lcaCommitId = lcaCommit.getId();

        if (lcaCommitId.equals(targetBranchHeadCommit.getId())) {
            exit("Given branch is an ancestor of the current branch.");
        }
        if (lcaCommitId.equals(HEADCommit.get().getId())) {
            checkoutCommit(targetBranchHeadCommit);
            setCurrentBranch(targetBranchName);
            exit("Current branch fast-forwarded.");
        }

        boolean hasConflict = false;

        Map<String, String> HEADCommitTrackedFilesMap = new HashMap<>(HEADCommit.get().getTracked());
        Map<String, String> targetBranchHeadCommitTrakcedFilesMap = targetBranchHeadCommit.getTracked();
        Map<String, String> lcaCommitTrackedFilesMap = lcaCommit.getTracked();

        for (Map.Entry<String, String> entry : lcaCommitTrackedFilesMap.entrySet()) {
            String filePath = entry.getKey();
            File file = new File(filePath);
            String blobId = entry.getValue();

            String targetBranchHeadCommitBlobId = targetBranchHeadCommitTrakcedFilesMap.get(filePath);
            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(filePath);

            if (targetBranchHeadCommitBlobId != null) { // exists in the target branch
                if (!targetBranchHeadCommitBlobId.equals(blobId)) { // modified in the target branch
                    if (HEADCommitBlobId != null) { // exists in the current branch
                        if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                            // case 1
                            Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                            stagingArea.get().add(file);
                        } else { // modified in the current branch
                            if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                                // case 8
                                hasConflict = true;
                                String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                                writeContents(file, conflictContent);
                                stagingArea.get().add(file);
                            } // else modified in the same ways
                            // case 3
                        }
                    } else { // deleted in current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(null, targetBranchHeadCommitBlobId);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                } // else not modified in the target branch
                // case 2, case 7
            } else { // deleted in the target branch
                if (HEADCommitBlobId != null) { // exists in the current branch
                    if (HEADCommitBlobId.equals(blobId)) { // not modified in the current branch
                        // case 6
                        stagingArea.get().remove(file);
                    } else { // modified in the current branch
                        // case 8
                        hasConflict = true;
                        String conflictContent = getConflictContent(HEADCommitBlobId, null);
                        writeContents(file, conflictContent);
                        stagingArea.get().add(file);
                    }
                } // else deleted in both branches
                // case 3
            }

            HEADCommitTrackedFilesMap.remove(filePath);
            targetBranchHeadCommitTrakcedFilesMap.remove(filePath);
        }

        for (Map.Entry<String, String> entry : targetBranchHeadCommitTrakcedFilesMap.entrySet()) {
            String targetBranchHeadCommitFilePath = entry.getKey();
            File targetBranchHeadCommitFile = new File(targetBranchHeadCommitFilePath);
            String targetBranchHeadCommitBlobId = entry.getValue();

            String HEADCommitBlobId = HEADCommitTrackedFilesMap.get(targetBranchHeadCommitFilePath);

            if (HEADCommitBlobId != null) { // added in both branches
                if (!HEADCommitBlobId.equals(targetBranchHeadCommitBlobId)) { // modified in different ways
                    // case 8
                    hasConflict = true;
                    String conflictContent = getConflictContent(HEADCommitBlobId, targetBranchHeadCommitBlobId);
                    writeContents(targetBranchHeadCommitFile, conflictContent);
                    stagingArea.get().add(targetBranchHeadCommitFile);
                } // else modified in the same ways
                // case 3
            } else { // only added in the target branch
                // case 5
                Blob.fromFile(targetBranchHeadCommitBlobId).writeContentToSource();
                stagingArea.get().add(targetBranchHeadFile);
            }
        }

        String newCommitMessage = "Merged" + " " + targetBranchName + " " + "into" + " " + currentBranch.get() + ".";
        commit(newCommitMessage, targetBranchHeadCommit.getId());

        if (hasConflict) {
            message("Encountered a merge conflict.");
        }
    }

    /**
     * Get the id of the latest ancestor of the two commits.
     *
     * @param commitA Commit instance
     * @param commitB Commit instance
     * @return Commit SHA1 id
     */
    @SuppressWarnings("ConstantConditions")
    private static Commit getLatestCommonAncestorCommit(Commit commitA, Commit commitB) {
        Comparator<Commit> commitComparator = Comparator.comparing(Commit::getDate).reversed();
        Queue<Commit> commitsQueue = new PriorityQueue<>(commitComparator);
        commitsQueue.add(commitA);
        commitsQueue.add(commitB);
        Set<String> checkedCommitIds = new HashSet<>();
        while (true) {
            Commit latestCommit = commitsQueue.poll();
            List<String> parentCommitIds = latestCommit.getParents();
            String firstParentCommitId = parentCommitIds.get(0);
            Commit firstParentCommit = Commit.fromFile(firstParentCommitId);
            if (checkedCommitIds.contains(firstParentCommitId)) {
                return firstParentCommit;
            }
            commitsQueue.add(firstParentCommit);
            checkedCommitIds.add(firstParentCommitId);
        }
    }

    /**
     * Merge the conflicted content and return a new String.
     *
     * @param currentBlobId Current Blob SHA1 id
     * @param targetBlobId  Target Blob SHA1 id
     * @return New content
     */
    private static String getConflictContent(String currentBlobId, String targetBlobId) {
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<<<<<<< HEAD").append("\n");
        if (currentBlobId != null) {
            Blob currentBlob = Blob.fromFile(currentBlobId);
            contentBuilder.append(currentBlob.getContentAsString());
        }
        contentBuilder.append("=======").append("\n");
        if (targetBlobId != null) {
            Blob targetBlob = Blob.fromFile(targetBlobId);
            contentBuilder.append(targetBlob.getContentAsString());
        }
        contentBuilder.append(">>>>>>>");
        return contentBuilder.toString();
    }

}
