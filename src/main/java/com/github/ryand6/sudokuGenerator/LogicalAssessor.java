package com.github.ryand6.sudokuGenerator;

import java.util.HashMap;

/*
    Used to assess the validity of the problem through human logical solving techniques, rejecting
    problems not solvable without backtracking/guess work. Determines difficulty rating for valid
    problems based on strategies deployed to solve.
 */
public class LogicalAssessor {

    private int [][] grid;
    private HashMap<String, Integer> strategyMap;
    private String rating;

    public LogicalAssessor(int [][] grid) {
        this.grid = grid;
        this.strategyMap = new HashMap<>();
    }

    // Determines difficulty rating based on strategies used for solution
    private void setRating() {

    }

    public static void main(String[] args) {

    }
}
