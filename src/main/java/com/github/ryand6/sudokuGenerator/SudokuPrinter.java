package com.github.ryand6.sudokuGenerator;

import java.util.Arrays;

public interface SudokuPrinter {

    default void printGrid(int[][] grid) {
        System.out.println(Arrays.deepToString(grid).replace("], ", "]\n"));
    }

}
