package com.github.ryand6.sudokuGenerator;

// Uses backtracking algorithm to find puzzle with unique solution
public class SudokuBacktracker {

    /*
    Check that the value in the cell doesn't violate Sudoku rules
     */
    public boolean validateCell(int[][] grid, int val, int row, int col) {

        // Get cell position of upper left corner of block value is in
        int blockRow = (int) Math.floor(row / 3) * 3;
        int blockCol = (int) Math.floor(col / 3) * 3;

        // Check column
        for (int x = 0; row < 9; row++) {
            if (grid[row][x] == val) {
                return false;
            }
        }

        // Check row
        for (int y = 0; col < 9; col++) {
            if (grid[y][col] == val) {
                return false;
            }
        }

        // Check block
        for (int p = 0; p < 3; p++) {
            for (int q = 0; q < 3; q++) {
                if (grid[blockRow + p][blockCol + 3] == val) {
                    return false;
                }
            }
        }

        return true;
    }


    public int[][] backtrackSolver(int[][] grid, int i, int j) {
        return grid;
    }

}
