package com.github.ryand6.sudokuGenerator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SudokuGenerator {

    static final int MIN_CLUES = 25;
    static final int MAX_CLUES = 39;
    private static final int MAX_CHECKS = 500;
    private static final Random RANDOM = new Random();

    // Used in the priority queue, where the higher the priority value, the more likely the board state is to produce a valid puzzle
    private static class BoardState {

        private final int[][] board;
        private final int priority;
        private final List<Integer> untriedCells;

        public BoardState(int[][] board, int priority, List<Integer> untriedCells) {
            this.board = board;
            this.priority = priority;
            this.untriedCells = untriedCells;
        }
        public int[][] getBoard() {
            return board;
        }
        public int getPriority() {
            return priority;
        }
        public List<Integer> getUntriedCells() {
            return untriedCells;
        }
    }

    public static void generateSudoku(String outputFilePath) {
        GridGenerator gridGenerator = new GridGenerator();
        gridGenerator.generateGrid();
        int[][] sudokuBoard = copyBoard(gridGenerator.getGrid());
        int targetClues = pickTargetClues();
        int[][] modifiedBoard = removeClues(sudokuBoard, targetClues, MIN_CLUES, RANDOM);
        if (modifiedBoard == null) {
            System.out.println("No solution could be found");
            return;
        }
        if (!new SudokuBacktracker().isUnique(modifiedBoard)) {
            System.out.println("Puzzle does not have a unique solution, discarding");
            return;
        }
        System.out.println("Final Board: ");
        System.out.println(Arrays.deepToString(modifiedBoard));
        LogicalAssessor logicalAssessor = new LogicalAssessor();
        if (!logicalAssessor.solve(modifiedBoard)) {
            System.out.println("Puzzle cannot be solved using logic alone, discarding");
            return;
        }
        String difficulty = getDifficultyRating(logicalAssessor);
        System.out.println("Difficulty: " + difficulty + "\n");
        if (difficulty == null) {
            System.out.println("Invalid difficulty setting, board not written to file");
            return;
        }
        int minCluesAllowed = getMinimumCluesForDifficulty(difficulty);
        int cluesRemaining = 81 - cellsToSolve(modifiedBoard);
        if (cluesRemaining < minCluesAllowed) {
            System.out.println("Clue count " + cluesRemaining + " below minimum of " + minCluesAllowed + " for difficulty " + difficulty + ", discarding puzzle");
            return;
        }
        writeToFile(sudokuBoard, modifiedBoard, difficulty, outputFilePath);

    }

    private static List<Integer> shuffledCellIndices(Random random) {
        List<Integer> cells = new ArrayList<>();
        // 41 pairs of cells, with 40 being the centre cell (its own opposite)
        for (int i = 0; i <= 40; i++) {
            cells.add(i);
        }
        Collections.shuffle(cells, random);
        return cells;
    }

    static int pickTargetClues() {
        return RANDOM.nextInt(MIN_CLUES, MAX_CLUES + 1);
    }


    static int[][] removeClues(int[][] board, int targetClues, int minClues, Random random) {
        // Priority Queue used to hold previous states of modified board in order of priority, calculated using calculatePriority method - this is used so more promising
        // board states can be tried if the current board is at an unsolvable/non-unique dead end
        PriorityQueue<BoardState> queue = new PriorityQueue<>(Comparator.comparingInt(BoardState::getPriority).reversed());
        int[][] start = copyBoard(board);

        SudokuBacktracker sudokuBacktracker = new SudokuBacktracker();
        LogicalAssessor logicalAssessor = new LogicalAssessor();

        queue.add(new BoardState(start, evaluatePriority(start), shuffledCellIndices(random)));

        int checks = 0;

        while (!queue.isEmpty()) {
            BoardState state = queue.peek();

            if (81 - cellsToSolve(state.getBoard()) <= targetClues) {
                return copyBoard(state.getBoard());
            }

            if (state.getUntriedCells().isEmpty()) {
                queue.poll();
                continue;
            }

            int index = state.getUntriedCells().remove(state.getUntriedCells().size() - 1);
            int row = index / 9;
            int col = index % 9;
            int oppRow = 8 - row;
            int oppCol = 8 - col;

            int[][] candidate = copyBoard(state.getBoard());
            if (candidate[row][col] == 0 || candidate[oppRow][oppCol] == 0) {
                continue;
            }
            candidate[row][col] = 0;
            candidate[oppRow][oppCol] = 0;

            if (81 - cellsToSolve(candidate) < minClues) {
                continue;
            }

            if (++checks > MAX_CHECKS) {
                return null;
            }

            boolean valid = cellsToSolve(candidate) < 4 || (sudokuBacktracker.isUnique(candidate) && logicalAssessor.solve(candidate));

            if (valid) {
                queue.add(new BoardState(candidate, evaluatePriority(candidate), shuffledCellIndices(random)));
            }
        }

        return null;
    }

    // Create a copy of a nested array
    private static int[][] copyBoard(int[][] board) {
        int[][] copy = new int[9][9];
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                copy[row][col] = board[row][col];
            }
        }
        return copy;
    }

    // Get count of cells left to solve on the board
    private static int cellsToSolve(int[][] board) {
        int count = 0;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (board[row][col] == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    // Heuristic approach to calculating the likelihood of a board state producing a valid puzzle
    private static int evaluatePriority(int[][] board) {
        int priority = cellsToSolve(board);
        int[][] keyCells = { {0,0}, {0,8}, {8,0}, {8,8}, {4,4}, {1,1}, {1,7}, {7,1}, {7,7} };
        for (int[] c : keyCells) {
            if (board[c[0]][c[1]] == 0) {
                priority--;
            }
        }
        return priority;
    }


    // Use list of currently developed strategies and whether they were used to solve the solution to determine the difficulty rating of the puzzle
    private static String getDifficultyRating(LogicalAssessor logicalAssessor) {
        HashMap<String, Integer> strategyMap = logicalAssessor.getStrategyMap();
        System.out.println(strategyMap);
        if (strategyMap.get("Swordfish") > 0 || strategyMap.get("Y Wing") > 0 || strategyMap.get("Simple Colouring") > 0) {
            return "Extreme";
        } else if (strategyMap.get("X Wing") > 0 || strategyMap.get("Intersection") > 0 || strategyMap.get("Hidden Triple") > 0 || strategyMap.get("Hidden Pair") > 0) {
            return "Hard";
        } else if (strategyMap.get("Naked Triple") > 0 || strategyMap.get("Naked Pair") > 0 || strategyMap.get("Hidden Single") > 0) {
            return "Medium";
        } else if (strategyMap.get("Basic Elimination") > 0) {
            return "Easy";
        } else {
            return null;
        }
    }

    // Write the completed board, it's puzzle state version, and it's uid to file. The file it's written to depends on the difficulty rating.
    private static void writeToFile(int[][] completedBoard, int[][] modifiedBoard, String difficulty, String outputFilePath) {
        String fileName = null;
        // File to write to depends on the difficulty of the puzzle
        switch(difficulty) {
            case "Easy":
                fileName = "easysudoku.tsv";
                break;
            case "Medium":
                fileName = "mediumsudoku.tsv";
                break;
            case "Hard":
                fileName = "hardsudoku.tsv";
                break;
            case "Extreme":
                fileName = "extremesudoku.tsv";
                break;
        }
        if (fileName == null) {
            System.out.println("No valid difficulty setting provided");
            return;
        }
        // Set output directory path object
        Path basePath = Paths.get(outputFilePath);
        // Get full path object by combining with the output filename
        Path fullPath = basePath.resolve(fileName);
        // Create file object using fullPath path object
        File file = fullPath.toFile();
        // Create the file if it doesn't currently exist
        try {
            if (!file.exists()) {
                if (file.createNewFile()) {
                    System.out.println("File created successfully: " + fullPath);
                    // Add headers to the file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        writer.write("PuzzleBoard\tSolvedBoard");
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } ;
                } else {
                    System.out.println("Failed to create the file: " + fullPath);
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return; // Exit the program or handle the error as needed
        }

        Set<String> existingPuzzles = loadExistingUids(file);

        String initialBoardString = generateBoardString(modifiedBoard);

        if (existingPuzzles.contains(initialBoardString)) {
            System.out.println("Uid found in file, board not appended to file.");
            return;
        }

        String completedBoardString = generateBoardString(completedBoard);

        // Validate both strings are exactly 81 characters before writing
        if (initialBoardString.length() != 81 || completedBoardString.length() != 81) {
            System.out.println("Board string length invalid - puzzle: " + initialBoardString.length() + ", solution: " + completedBoardString.length() + ". Board not written to file.");
            return;
        }

        // Used buffered writer in append mode to add the new board state, it's puzzle state, and the uid to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(initialBoardString + "\t" + completedBoardString);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        };

    }

    // Read all the file's uids into memory
    private static Set<String> loadExistingUids(File file) {
        Set<String> ids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip header
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 2) {
                    ids.add(parts[0]);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    // Create board string representation for writing to file
    private static String generateBoardString(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) {
            for (int num : row) {
                if (num == 0) {
                    sb.append('.');
                } else {
                    sb.append(num);
                }
            }
        }
        return sb.toString();
    }

    private static int getMinimumCluesForDifficulty(String difficulty) {
        return switch (difficulty) {
            case "Easy" -> 35;
            case "Medium" -> 30;
            case "Hard", "Extreme" -> 25;
            default -> 0;
        };
    }

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        String outputDir = null;
        boolean isValidPath = false;
        int iterations = 0;

        while (!isValidPath) {
            System.out.println("Please enter the output directory where the puzzles will be saved to, or type 'quit' to cancel: ");
            outputDir = scanner.nextLine();
            if (outputDir.equals("quit")) {
                return;
            }
            Path path = Paths.get(outputDir);
            if (!Files.isExecutable(path)) {
                System.out.println("The specified directory does not exist.");
                continue;
            } else {
                isValidPath = true;
            }
        }

        boolean validIterations = false;
        while (!validIterations) {
            System.out.println("Please enter number of puzzles to attempt to generate. Note that this may not be the exact number of puzzles created, in case duplicates are found or there are any failures generated the puzzle:");
            if (scanner.hasNextInt()) {
                iterations = scanner.nextInt();
                if (iterations > 0) {
                    validIterations = true;
                } else {
                    System.out.println("Please enter a positive numeric value.");
                }
            } else {
                System.out.println("Invalid input. Please enter a positive numeric value.");
                // clear the token in the input buffer
                scanner.next();
            }
        }

        for (int i = 0; i < iterations; i++) {
            SudokuGenerator.generateSudoku(outputDir);
        }
    }

}
