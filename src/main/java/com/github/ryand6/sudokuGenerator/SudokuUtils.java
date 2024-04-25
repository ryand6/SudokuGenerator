package com.github.ryand6.sudokuGenerator;

import java.util.Arrays;

public interface SudokuUtils {

    /*
    Check that the value in the cell doesn't violate Sudoku rules
     */
    default boolean validateCell(int[][]grid, int val, int row, int col) {

        // Get cell position of upper left corner of block value is in
        int blockRow = (int) Math.floor(row / 3) * 3;
        int blockCol = (int) Math.floor(col / 3) * 3;

        // Check column
        for (int x = 0; x < 9; x++) {
            if (grid[row][x] == val) {
                return false;
            }
        }

        // Check row
        for (int y = 0; y < 9; y++) {
            if (grid[y][col] == val) {
                return false;
            }
        }

        // Check block
        for (int p = 0; p < 3; p++) {
            for (int q = 0; q < 3; q++) {
                if (grid[blockRow + p][blockCol + q] == val) {
                    return false;
                }
            }
        }

        return true;
    }


    default void printGrid(int[][] grid) {
        System.out.println(Arrays.deepToString(grid).replace("], ", "]\n"));
    }

}
