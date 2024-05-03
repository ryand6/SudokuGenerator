package com.github.ryand6.sudokuGenerator;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertTrue;

public class SudokuLogicalStrategyTest {

    @Test
    public void testBasicPuzzles_SimpleEliminationOnly() {

        int[][] sudokuGrid1 = {
            {5, 3, 4, 6, 0, 8, 9, 1, 2},
            {6, 7, 2, 1, 9, 5, 3, 4, 8},
            {1, 9, 8, 3, 4, 2, 5, 6, 7},
            {8, 5, 9, 7, 6, 1, 4, 2, 3},
            {0, 2, 6, 8, 5, 3, 7, 9, 0},
            {7, 1, 3, 9, 2, 4, 8, 5, 6},
            {9, 6, 1, 5, 3, 7, 2, 8, 4},
            {2, 8, 7, 4, 1, 9, 6, 3, 5},
            {3, 4, 5, 2, 0, 6, 1, 7, 9}
        };

        LogicalAssessor solver = new LogicalAssessor();
        solver.solve(sudokuGrid1);
        HashMap<String, Integer> sMap = solver.getStrategyMap();
        int eliminationCount = sMap.get("Basic Elimination");
        // Expect 8 candidates to be removed for each blank cell
        assertTrue(eliminationCount == 32);

    }

    @Test
    public void testHiddenSingle_HiddenSingleExists() {
        int[][] sudokuGrid1 = {
                {0, 2, 8, 0, 0, 7, 0, 0, 0},
                {0, 1, 6, 0, 8, 3, 0, 7, 0},
                {0, 0, 0, 0, 2, 0, 8, 5, 1},
                {1, 3, 7, 2, 9, 0, 0, 0, 0},
                {0, 0, 0, 7, 3, 0, 0, 0, 0},
                {0, 0, 0, 0, 4, 6, 3, 0, 7},
                {2, 9, 0, 0, 7, 0, 0, 0, 0},
                {0, 0, 0, 8, 6, 0, 1, 4, 0},
                {0, 0, 0, 3, 0, 0, 7, 0, 0}
        };

        LogicalAssessor solver = new LogicalAssessor();
        solver.solve(sudokuGrid1);
        HashMap<String, Integer> sMap = solver.getStrategyMap();
        int eliminationCount = sMap.get("Hidden Single");
        System.out.println(eliminationCount);
        assertTrue(eliminationCount == 2);
    }

    @Test
    public void testNakedPairs() {
        int[][] sudokuGrid1 = {
                {4, 0, 0, 0, 0, 0, 9, 3, 8},
                {0, 3, 2, 0, 9, 4, 1, 0, 0},
                {0, 9, 5, 3, 0, 0, 2, 4, 0},
                {3, 7, 0, 6, 0, 9, 0, 0, 4},
                {5, 2, 9, 0, 0, 1, 6, 7, 3},
                {6, 0, 4, 7, 0, 3, 0, 9, 0},
                {9, 5, 7, 0, 0, 8, 3, 0, 0},
                {0, 0, 3, 9, 0, 0, 4, 0, 0},
                {2, 4, 0, 0, 3, 0, 7, 0, 9}
        };

        LogicalAssessor solver = new LogicalAssessor();
        solver.solve(sudokuGrid1);
        HashMap<String, Integer> sMap = solver.getStrategyMap();
        int eliminationCount = sMap.get("Naked Pair");
        System.out.println(eliminationCount);
        assertTrue(eliminationCount > 0);
    }

}
