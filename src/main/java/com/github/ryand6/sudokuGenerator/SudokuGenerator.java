package com.github.ryand6.sudokuGenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SudokuGenerator {

    // Used in the priority queue, where the higher the priority value, the more likely the board state is to produce a valid puzzle
    private static class BoardState {

        private final int[][] board;
        private final int priority;

        public BoardState(int[][] board, int priority) {
            this.board = board;
            this.priority = priority;
        }
        public int[][] getBoard() {
            return board;
        }
        public int getPriority() {
            return priority;
        }
    }

    public static void generateSudoku(String outputFilePath) {

        GridGenerator gridGenerator = new GridGenerator();
        gridGenerator.generateGrid();
        int[][] sudokuBoard = gridGenerator.getGrid();
        int[][] modifiedBoard = removeClues(sudokuBoard);
        if (modifiedBoard == null) {
            System.out.println("No solution could be found");
            return;
        }
        System.out.println("Final Board: ");
        System.out.println(Arrays.deepToString(modifiedBoard));
        // Initialise class used to solve the board through human strategies to determine a difficulty rating and ensure it can be solved via logic
        LogicalAssessor logicalAssessor = new LogicalAssessor();
        logicalAssessor.solve(modifiedBoard);
        String difficulty = getDifficultyRating(logicalAssessor);
        System.out.println("Difficulty: " + difficulty + "\n");
        if (difficulty == null) {
            System.out.println("Invalid difficulty setting, board not written to file");
            return;
        }
        try {
            writeToFile(sudokuBoard, modifiedBoard, difficulty, outputFilePath);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    private static int[][] removeClues(int[][] board) {
        // Priority Queue used to hold previous states of modified board in order of priority, calculated using calculatePriority method - this is used so more promising
        // board states can be tried if the current board is at an unsolvable/non-unique dead end
        PriorityQueue<BoardState> boardQueue = new PriorityQueue<>(Comparator.comparingInt(BoardState::getPriority));
        int[][] modifiedBoard = copyBoard(board);
        // Set the desired number of clues to be left on the generated board
        int numberOfCluesAllowed = new Random().nextInt(20, 40);

        // Initialise class used to test whether the board in its current state has a unique solution
        SudokuBacktracker sudokuBacktracker = new SudokuBacktracker();
        // Initialise class used to solve the board through human strategies to determine a difficulty rating and ensure it can be solved via logic
        LogicalAssessor logicalAssessor = new LogicalAssessor();

        int maxAttempts = 100;
        int currentAttempts = 0;

        while (cellsToSolve(modifiedBoard) < 81 - numberOfCluesAllowed) {
            currentAttempts++;
            if (currentAttempts > maxAttempts) {
                System.out.println("Backtracking failed, no solution could be found.");
                return null;
            }
            boolean progress = false;

            // Get cell opposite cell coordinates for board symmetry when removing clues
            int cellToRemoveRow = new Random().nextInt(9);
            int cellToRemoveCol = new Random().nextInt(9);
            int opposingCellToRemoveRow = 8 - cellToRemoveRow;
            int opposingCellToRemoveCol = 8 - cellToRemoveCol;

            // Store clues in case they need to be added back for reasons such as puzzle having more than one solution, or not being able to be solved using the logical assessor
            int removedClue = modifiedBoard[cellToRemoveRow][cellToRemoveCol];
            int oppositeRemovedClue = modifiedBoard[opposingCellToRemoveRow][opposingCellToRemoveCol];
            // Reset loop if the randomly selected cells have already had their clues removed
            if (modifiedBoard[cellToRemoveRow][cellToRemoveCol] == 0 || modifiedBoard[opposingCellToRemoveRow][opposingCellToRemoveCol] == 0) {
                continue;
            }
            // Remove clues
            modifiedBoard[cellToRemoveRow][cellToRemoveCol] = 0;
            modifiedBoard[opposingCellToRemoveRow][opposingCellToRemoveCol] = 0;
            // 77 is the maximum number of clues on a board that can still yield a non-unique solution, therefore uniqueness and logic checks not required if the number of cells to solve is less than the number of total cells minus the max number of clues
            if (cellsToSolve(modifiedBoard) < 4) {
                progress = true;
            }
            // Add the recently removed clues back to the board and reset the loop if either the puzzle has more than one solution, or it can't be solved by the logical assessor
            if (sudokuBacktracker.isUnique(modifiedBoard) && logicalAssessor.solve(modifiedBoard)) {
                progress = true;
            }
            if (progress) {
                BoardState newState = new BoardState(copyBoard(modifiedBoard), evaluatePriority(copyBoard(modifiedBoard)));
                boardQueue.offer(newState);
            } else {
                modifiedBoard = boardQueue.peek().getBoard();
            }
        }
        return modifiedBoard;
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
        int priority = 0;

        priority += cellsToSolve(board); // Higher priority for fewer filled cells

        // Penalize boards with fewer filled cells in certain regions (e.g., corners, center)
        int[][] regions = {
                {0, 0}, {0, 8}, {8, 0}, {8, 8}, // corners
                {4, 4}, {1, 1}, {1, 7}, {7, 1}, {7, 7} // center and near-center cells
        };
        for (int[] region : regions) {
            if (board[region[0]][region[1]] == 0) {
                // Penalize boards with fewer filled cells in important regions
                priority--;
            }
        }

        // Consider the difficulty of solving the puzzle
        LogicalAssessor logicalAssessor = new LogicalAssessor();
        if (logicalAssessor.solve(board)) {
            // Higher priority for puzzles that are easier to solve
            priority += 50;
        } else {
            // Penalize puzzles that cannot be solved
            priority -= 100;
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
    private static void writeToFile(int[][] completedBoard, int[][] modifiedBoard, String difficulty, String outputFilePath) throws JsonProcessingException {
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

        // Get all the uids from the file
        Set<BigInteger> uids = loadExistingUids(file);
        BigInteger boardUniqueId = generateUniqueBoardId(modifiedBoard);

        // Don't add duplicate boards to the file
        if (uids.contains(boardUniqueId)) {
            System.out.println("Uid found in file, board not appended to file.");
            return;
        }

        String initialBoardString = generateBoardString(modifiedBoard);
        String completedBoardString = generateBoardString(completedBoard);

        // Create object mapper object used for serialising nested array to JSON format
        ObjectMapper mapper = new ObjectMapper();

        // Used buffered writer in append mode to add the new board state, it's puzzle state, and the uid to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(initialBoardString + "\t" + completedBoardString);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        };

    }

    // Read all the file's uids into memory
    private static Set<BigInteger> loadExistingUids(File file) {
        Set<BigInteger> ids = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Skip header
            String line = reader.readLine();
            while((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 3) {
                    ids.add(new BigInteger(parts[2]));
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    // Create unique id of the puzzle board used for lookup in the files to ensure no duplicate puzzles are written to file
    private static BigInteger generateUniqueBoardId(int[][] modifiedBoard) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : modifiedBoard) {
            for (int num : row) {
                sb.append(num);
            }
        }
        return new BigInteger(sb.toString());
    }

    // Create board string representation for writing to file
    private static String generateBoardString(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : board) {
            for (int num : row) {
                sb.append(num);
            }
        }
        return sb.toString();
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
