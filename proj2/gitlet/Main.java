package gitlet;

import static gitlet.MyUtils.exit;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Jeffrey
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            exit("Please enter a command.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.init();
                break;
            case "add":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String addFileName = args[1];
                new Repository().add(addFileName);
                break;
            case "commit":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String commitMessage = args[1];
                if (commitMessage.isEmpty()) {
                    exit("Please enter a commit message.");
                }
                new Repository().commit(commitMessage);
                break;
            case "rm":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String rmFileName = args[1];
                new Repository().remove(rmFileName);
                break;
            case "log":
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().log();
                break;
            case "global-log":
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "find":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String message = args[1];
                if (message.isEmpty()) {
                    exit("Found no commit with that message.");
                }
                Repository.find(message);
                break;
            case "status":
                Repository.checkWorkingDir();
                validateNumArgs(args, 1);
                new Repository().status();
                break;
            case "checkout":
                Repository.checkWorkingDir();
                Repository repository = new Repository();
                switch (args.length) {
                    case 3:
                        if (!args[1].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        String fileName = args[2];
                        repository.checkout(fileName);
                        break;
                    case 4:
                        if (!args[2].equals("--")) {
                            exit("Incorrect operands.");
                        }
                        String commitId = args[1];
                        String fileName1 = args[3];
                        repository.checkout(commitId, fileName1);
                        break;
                    case 2:
                        String branch = args[1];
                        repository.checkoutBranch(branch);
                        break;
                    default:
                        exit("Incorrect operands");
                }
                break;
            case "branch":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String branchName = args[1];
                new Repository().branch(branchName);
                break;
            case "rm-branch":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String rmBranchName = args[1];
                new Repository().rmBranch(rmBranchName);
                break;
            case "reset":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String resetCommitId = args[1];
                new Repository().reset(resetCommitId);
                break;
            case "merge":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String mergeBranchName = args[1];
                new Repository().merge(mergeBranchName);
                break;
            default:
                exit("No command with that name exists.");
        }
    }

    /**
     * Checks the number of arguments versus the expected number.
     *
     * @param args Argument array from command line
     * @param n    Number of expected arguments
     */
    private static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            exit("Incorrect operands.");
        }
    }
}
