package com.github.ryand6.sudokuGenerator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SudokuGeneratorTest {

    private static final String UNIQUE_PUZZLE = "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79";
    private static final String UNIQUE_SOLUTION = "534678912672195348198342567859761423426853791713924856961537284287419635345286179";

    static int[][] parse(String s) {
        assertEquals(81, s.length(), "board string must be 81 characters");
        int[][] grid = new int[9][9];
        for (int i = 0; i < 81; i++) {
            char c = s.charAt(i);
            grid[i / 9][i % 9] = (c == '.') ? 0 : c - '0';
        }
        return grid;
    }

    static int countClues(int[][] board) {
        int clues = 0;
        for (int[] row : board) {
            for (int v : row) {
                if (v != 0) clues++;
            }
        }
        return clues;
    }

    static int[][] copy(int[][] board) {
        int[][] copy = new int[9][];
        for (int i = 0; i < 9; i++) copy[i] = board[i].clone();
        return copy;
    }

    private static int countPuzzleLines(Path dir) throws IOException {
        int lines = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*sudoku.tsv")) {
            for (Path file : files) {
                lines += Math.max(0, Files.readAllLines(file).size() - 1);   // minus the header
            }
        }
        return lines;
    }

    // Returns one message per problem found in any *sudoku.tsv file in the directory
    static List<String> findProblems(Path dir) throws IOException {
        List<String> problems = new ArrayList<>();
        SudokuBacktracker backtracker = new SudokuBacktracker();
        SudokuValidation validation = new SudokuValidation() {};

        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*sudoku.tsv")) {
            for (Path file : files) {
                List<String> lines = Files.readAllLines(file);
                for (int n = 1; n < lines.size(); n++) {   // index 0 is the header
                    String where = file.getFileName() + " line " + (n + 1);
                    String[] parts = lines.get(n).split("\t");
                    if (parts.length != 2 || parts[0].length() != 81 || parts[1].length() != 81) {
                        problems.add(where + ": MALFORMED");
                        continue;
                    }
                    int[][] puzzle = parse(parts[0]);
                    int[][] solution = parse(parts[1]);

                    if (!validation.validateGrid(solution)) {
                        problems.add(where + ": SOLUTION IS NOT A VALID GRID");
                        continue;
                    }
                    boolean givensMatch = true;
                    for (int r = 0; r < 9 && givensMatch; r++) {
                        for (int c = 0; c < 9; c++) {
                            if (puzzle[r][c] != 0 && puzzle[r][c] != solution[r][c]) {
                                givensMatch = false;
                                break;
                            }
                        }
                    }
                    if (!givensMatch) {
                        problems.add(where + ": GIVENS DO NOT MATCH SOLUTION");
                        continue;
                    }
                    if (!backtracker.isUnique(puzzle)) {
                        problems.add(where + ": NOT UNIQUE");
                    }
                }
            }
        }
        return problems;
    }

    private record Generated(int[][] fullGrid, int[][] fullGridSnapshot, int target, int[][] puzzle) {}

    private static final List<Generated> BATCH = new ArrayList<>();

    @BeforeAll
    static void generateBatch() {
        GridGenerator gridGenerator = new GridGenerator();
        for (int round = 0; round < 2; round++) {
            for (int target = 30; target <= 39; target++) {
                gridGenerator.generateGrid();
                int[][] full = copy(gridGenerator.getGrid());
                int[][] snapshot = copy(full);
                int[][] puzzle = SudokuGenerator.removeClues(full, target, SudokuGenerator.MIN_CLUES, new Random(1000L * round + target));
                if (puzzle != null) {
                    BATCH.add(new Generated(full, snapshot, target, puzzle));
                }
            }
        }
    }

    @Test
    void batchWasActuallyGenerated() {
        // Guards every property test below against passing vacuously on an empty batch
        assertTrue(BATCH.size() >= 10, "Only " + BATCH.size() + " puzzles were generated");
    }

    @Test
    void knownUniquePuzzleIsUnique() {
        assertTrue(new SudokuBacktracker().isUnique(parse(UNIQUE_PUZZLE)));
    }

    @Test
    void fullySolvedGridIsUnique() {
        assertTrue(new SudokuBacktracker().isUnique(parse(UNIQUE_SOLUTION)));
    }

    @Test
    void gridWithOneClueIsNotUnique() {
        int[][] grid = new int[9][9];
        grid[0][0] = 1;
        assertFalse(new SudokuBacktracker().isUnique(grid));
    }

    @Test
    void blankingTwoDigitsEntirelyIsNotUnique() {
        int[][] grid = parse(UNIQUE_SOLUTION);
        for (int[] row : grid) {
            for (int c = 0; c < 9; c++) {
                if (row[c] == 1 || row[c] == 2) row[c] = 0;
            }
        }
        assertFalse(new SudokuBacktracker().isUnique(grid));
    }

    @Test
    void backtrackerGivesCorrectAnswersWhenReused() {
        SudokuBacktracker backtracker = new SudokuBacktracker();
        int[][] empty = new int[9][9];
        assertFalse(backtracker.isUnique(empty));
        assertTrue(backtracker.isUnique(parse(UNIQUE_PUZZLE)));
        assertFalse(backtracker.isUnique(empty));
    }

    @Test
    void pickedTargetsStayInRangeAndCoverEveryValue() {
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            int target = SudokuGenerator.pickTargetClues();
            assertTrue(target >= SudokuGenerator.MIN_CLUES && target <= SudokuGenerator.MAX_CLUES, "Target " + target + " outside the allowed range");
            seen.add(target);
        }
        assertEquals(SudokuGenerator.MAX_CLUES - SudokuGenerator.MIN_CLUES + 1, seen.size(), "Not every value in the range was ever picked: " + seen);
    }

    @Test
    void everyReturnedPuzzleHasExactlyOneSolution() {
        SudokuBacktracker backtracker = new SudokuBacktracker();
        for (Generated g : BATCH) {
            assertTrue(backtracker.isUnique(g.puzzle()), "Non-unique puzzle (target " + g.target() + "): " + Arrays.deepToString(g.puzzle()));
        }
    }

    @Test
    void everyReturnedPuzzleIsSolvableByLogic() {
        for (Generated g : BATCH) {
            assertTrue(new LogicalAssessor().solve(g.puzzle()), "Not logically solvable (target " + g.target() + "): " + Arrays.deepToString(g.puzzle()));
        }
    }

    @Test
    void givensMatchTheOriginalSolvedGrid() {
        for (Generated g : BATCH) {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (g.puzzle()[r][c] != 0) {
                        assertEquals(g.fullGridSnapshot()[r][c], g.puzzle()[r][c], "Given at (" + r + "," + c + ") differs from the solved grid");
                    }
                }
            }
        }
    }

    @Test
    void inputGridIsNotModified() {
        for (Generated g : BATCH) {
            assertArrayEquals(g.fullGridSnapshot(), g.fullGrid());
        }
    }

    @Test
    void emptyCellsAreSymmetric() {
        for (Generated g : BATCH) {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    assertEquals(g.puzzle()[r][c] == 0, g.puzzle()[8 - r][8 - c] == 0, "Asymmetric at (" + r + "," + c + "): " + Arrays.deepToString(g.puzzle()));
                }
            }
        }
    }

    @Test
    void clueCountIsTargetOrOneBelowAndNeverBelowMinimum() {
        for (Generated g : BATCH) {
            int clues = countClues(g.puzzle());
            assertTrue(clues <= g.target() && clues >= g.target() - 1 && clues >= SudokuGenerator.MIN_CLUES, "Clues " + clues + " for target " + g.target());
        }
    }

    @Test
    void differentTargetsGiveVaryingClueCounts() {
        Set<Integer> clueCounts = new HashSet<>();
        for (Generated g : BATCH) {
            clueCounts.add(countClues(g.puzzle()));
        }
        assertTrue(clueCounts.size() >= 5, "Expected varied clue counts, got " + clueCounts);
    }

    @Test
    void minimumIsRespectedEvenWhenTheTargetIsBelowIt() {
        GridGenerator gridGenerator = new GridGenerator();
        gridGenerator.generateGrid();
        int[][] puzzle = SudokuGenerator.removeClues(gridGenerator.getGrid(), 10, 32, new Random(7));
        if (puzzle != null) {
            assertTrue(countClues(puzzle) >= 32, "Floor ignored: " + countClues(puzzle) + " clues");
            assertTrue(new SudokuBacktracker().isUnique(puzzle));
        }
    }

    @Test
    void lowTargetsEitherFailCleanlyOrReturnAValidPuzzle() {
        GridGenerator gridGenerator = new GridGenerator();
        SudokuBacktracker backtracker = new SudokuBacktracker();
        for (int seed = 0; seed < 3; seed++) {
            gridGenerator.generateGrid();
            int[][] puzzle = SudokuGenerator.removeClues(gridGenerator.getGrid(), SudokuGenerator.MIN_CLUES, SudokuGenerator.MIN_CLUES, new Random(seed));
            if (puzzle != null) {
                assertTrue(countClues(puzzle) >= SudokuGenerator.MIN_CLUES);
                assertTrue(backtracker.isUnique(puzzle), Arrays.deepToString(puzzle));
            }
        }
    }

    @Test
    void generateSudokuOnlyWritesValidUniquePuzzles(@TempDir Path dir) throws IOException {
        for (int i = 0; i < 8; i++) {
            SudokuGenerator.generateSudoku(dir.toString());
        }
        int written = countPuzzleLines(dir);
        assumeTrue(written > 0, "No puzzles were written in 8 attempts, nothing to check");
        List<String> problems = findProblems(dir);
        assertTrue(problems.isEmpty(), "Invalid puzzles written:\n" + String.join("\n", problems));
    }

    @Test
    void fileCheckDetectsNonUniquePuzzle(@TempDir Path dir) throws IOException {
        StringBuilder bad = new StringBuilder();
        for (char c : UNIQUE_SOLUTION.toCharArray()) {
            bad.append(c == '1' || c == '2' ? '.' : c);
        }
        Files.writeString(dir.resolve("easysudoku.tsv"), "PuzzleBoard\tSolvedBoard\n" + UNIQUE_PUZZLE + "\t" + UNIQUE_SOLUTION + "\n" + bad + "\t" + UNIQUE_SOLUTION + "\n");
        List<String> problems = findProblems(dir);
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("line 3") && problems.get(0).contains("NOT UNIQUE"), problems.get(0));
    }

}
