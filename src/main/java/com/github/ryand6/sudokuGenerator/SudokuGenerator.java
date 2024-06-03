package com.github.ryand6.sudokuGenerator;

import java.util.Random;

public class SudokuGenerator {

    public static void generateSudoku() {

        GridGenerator gridGenerator = new GridGenerator();
        gridGenerator.generateGrid();
        int[][] sudokuBoard = gridGenerator.getGrid();
        // Initialise class used to solve the board through human strategies to determine a difficulty rating and ensure it can be solved via logic
        LogicalAssessor logicalAssessor = new LogicalAssessor();
        int[][] modifiedBoard = removeClues(sudokuBoard, logicalAssessor);
        String difficulty = getDifficultyRating(logicalAssessor);

    }

    private static int[][] removeClues(int[][] board, LogicalAssessor logicalAssessor) {
        int[][] modifiedBoard = new int[9][9];
        // Copy generated board into new nested array for modifying
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                modifiedBoard[row][col] = board[row][col];
            }
        }
        // Set the desired number of clues to be left on the generated board
        int numberOfCluesAllowed = new Random().nextInt(20, 40);
        // Initialise class used to test whether the board in its current state has a unique solution
        SudokuBacktracker sudokuBacktracker = new SudokuBacktracker();
        while (cellsToSolve(modifiedBoard) > numberOfCluesAllowed + 1) {
            // Get cell opposite cell coordinates for board symmetry when removing clues
            int cellToRemoveRow = new Random().nextInt(8);
            int cellToRemoveCol = new Random().nextInt(8);
            int opposingCellToRemoveRow = Math.abs(4-cellToRemoveRow) * 2;
            int opposingCellToRemoveCol = Math.abs(4-cellToRemoveCol) * 2;
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
            // Add the recently removed clues back to the board and reset the loop if either the puzzle has more than one solution, or it can't be solved by the logical assessor
            if (!sudokuBacktracker.isUnique(modifiedBoard) || !logicalAssessor.solve(modifiedBoard)) {
                modifiedBoard[cellToRemoveRow][cellToRemoveCol] = removedClue;
                modifiedBoard[opposingCellToRemoveRow][opposingCellToRemoveCol] = oppositeRemovedClue;
                continue;
            }
        }
        return modifiedBoard;
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

    private static String getDifficultyRating(LogicalAssessor logicalAssessor) {
        return "easy";
    }

    private static void writeToFile() {

    }

}
