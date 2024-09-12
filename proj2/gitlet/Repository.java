package gitlet;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static gitlet.MyUtils.*;
import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Jeffrey
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

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
     * @param fileName Name of the file
     *
     * @return File instance
     */
    private static File getFileFromCWD(String fileName) {
        return Paths.get(fileName).isAbsolute() ? new File(fileName) : join(CWD, fileName);
    }

    /**
     * Get head commit of the branch.
     * @param branchName Name of the branch
     * @return Commit instance
     */
    private static Commit getBranchHeadCommit(String branchName) {
        File branchHeadFile = getBranchHeadFile(branchName);
        return getBranchHeadCommit(branchHeadFile);
    }

    /**
     * Get head commit of the branch.
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
}
