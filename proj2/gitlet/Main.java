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
                String message = args[1];
                if (message.isEmpty()) {
                    exit("Please enter a commit message.");
                }
                new Repository().commit(message);
                break;
            case "rm":
                Repository.checkWorkingDir();
                validateNumArgs(args, 2);
                String rmFileName = args[1];
                new Repository().remove(rmFileName);
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
