package sudoku;

/**
 * Place for your code.
 */
public class SudokuSolver {	

	/**
	 * @return names of the authors and their student IDs (1 per line).
	 */
	public String authors() {
		return "John Wiebe - 23749120";
	}

	/**
	 * Performs constraint satisfaction on the given Sudoku board using Arc Consistency and Domain Splitting.
	 * 
	 * @param board the 2d int array representing the Sudoku board. Zeros indicate unfilled cells.
	 * @return the solved Sudoku board
	 */
	public int[][] solve(int[][] board) {
		// Represent domain as a 3d boolean array. For example, domain[0][1][6] = true would mean that cell(0,1) has 6 in its domain
		// Index 0 for each domain is used in arc consistency function to determine if we have used that cell to limit the domain of other cells yet
		boolean domain[][][] = new boolean[9][9][10];
		
		// initialize domain
		for (int row=0; row<9; row++) {
			for(int col=0; col<9; col++) {
				// if the cell already has a value, set everything in its domain to false except that number and [0]
				if (board[row][col] != 0) {
					for (int d=1; d<10; d++) {
						domain[row][col][d] = false;
					}
					domain[row][col][board[row][col]] = true;
					domain[row][col][0] = true;
				}
				// if the cell doesn't have a value yet, everything is true initially
				else {
					for (int d=0; d<10; d++) {
						domain[row][col][d] = true;
					}
				}
			}
		}
		return domainSplit(board, domain);
	}
	
	/**
	 * performs arc consistency with domain splitting on a Sudoku board
	 * 
	 * @param board - a 2d array representing the Sudoku board
	 * @param domain - a 3d array representing the domain of each cell on the board
	 * @return the solved Sudoku board or null if it couldn't be solved
	 */
	private int[][] domainSplit(int[][] board, boolean[][][] domain) {
		// if arc consistency solves the board, you're done!
		if (arcConsistency(board, domain) == null) {
			return null;
		}
		else if (isSolved(domain)) {
			return board;
		}
		// perform domain splitting
		for (int row=0; row<9; row++) {
			for (int col=0; col<9; col++) {
				if (board[row][col] == 0) {
					for (int d=1; d<10; d++) {
						// pick a possible value for a cell
						if (domain[row][col][d]) {
							
							// save existing board and domain incase this doesn't work out
							int[][] origboard = copyBoard(board);
							boolean[][][] origdomain = copyDomain(domain);
							
							// set the chosen value for the chosen cell
							for (int i=1; i<10; i++) {
								domain[row][col][i] = false;
							}
							domain[row][col][0] = true;
							domain[row][col][d] = true;
							
							// perform arc consistency and domain splitting with this new set variable
							board = domainSplit(board, domain);
							
							if (board == null) {
								// using this value didn't work out. reset everything and remove it from the domain
								board = origboard;
								domain = origdomain;
								domain[row][col][d] = false;
							}
							else {
								return board;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Performs arc consistency on the board (hopefully) reducing the domain of each cell
	 * 
	 * @param board - the Sudoku board
	 * @param domain - a 3d boolean array where representing the domain of each cell at (row, column). 
	 * @return the reduced domain array
	 */
	private boolean[][][] arcConsistency(int[][] board, boolean[][][] domain) {
		boolean flag = true;
		// we want to continue fixing domains until nothings being changed anymore
		while (flag) {
			flag = false;
			for (int row=0; row<9; row++) {
				for (int col=0; col<9; col++) {
					// only consider cells if a value that we haven't looked at yet
					if ((board[row][col] != 0) && (domain[row][col][0])) {
						// remove current cell value from the domain of other cells in the ROW
						for (int i=0; i<9; i++) {
							if ((i != col) && (domain[row][i][board[row][col]])) {
								domain[row][i][board[row][col]] = false;
								flag = true;
							}
						}
						// remove current cell value from the domain of other cells in the COLUMN
						for (int i=0; i<9; i++) {
							if ((i != row) && (domain[i][col][board[row][col]])) {
								domain[i][col][board[row][col]] = false;
								flag = true;
							}
						}
						
						// find which SQUARE the current cell is in and remove it's value from the domain of the others in the SQUARE
						float rowfloat = row;
						float colfloat = col;
						int rowpos;
						if (rowfloat/2 <= 1) {
							rowpos = 0;
						}
						else if (rowfloat/2 < 3) {
							rowpos = 3;
						}
						else {
							rowpos = 6;
						}
						
						int colpos;
						if (colfloat/2 <= 1) {
							colpos = 0;
						}
						else if (colfloat/2 < 3) {
							colpos = 3;
						}
						else {
							colpos = 6;
						}
						
						for (int i=0; i<3; i++) {
							for (int j=0; j<3; j++) {
								if ((rowpos+i != row) && (colpos+j != col) && (domain[rowpos+i][colpos+j][board[row][col]])) {
									domain[rowpos+i][colpos+j][board[row][col]] = false;
									flag = true;
								}
							}
						}
						// we don't have to consider this cell anymore
						domain[row][col][0] = false;
					}
					
					if (board[row][col] == 0) {
						// check if we can assign a value to this cell from it's now narrowed domain
						int val = oneOrNone(domain[row][col]);
						if (val == -1) {
							// cell has no possible values
							return null;
						}
						else if (val != 0) {
							// cell only has one value!
							board[row][col] = val;
							flag = true;
						}
					}
				}
			}
		}
		return domain;
	}
	
	/**
	 * Checks if a given 3d board has been solved or not
	 * 
	 * @param a 3d int array representing a sudoku board with domain for each cell
	 * @return true if board is solved, false otherwise
	 */
	private boolean isSolved(boolean[][][] board) {
		for (int row=0; row<9; row++) {
			for (int col=0; col<9; col++) {
				if (oneOrNone(board[row][col]) < 1) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * determine if the passed in domain has a single possible value or no possible value
	 * 
	 * @param domain - the domain of a single Sudoku board cell
	 * @return if the domain only has one value, that value is returned
	 * 		   if the domain has no possible values, -1 is returned
	 * 		   if the domain has multiple possible values, 0 is returned
	 */
	private int oneOrNone(boolean[] domain) {
		int val = 0;
		for (int i=1; i<10; i++) {
			if (domain[i] && (val == 0)) {
				val = i;
			}
			else if (domain[i] && (val > 0)) {
				return 0;
			}
		}
		// we didn't find any values in the domain
		if (val == 0) {
			return -1;
		}
		return val;
	}
	
	/**
	 * copies the supplied board
	 * 
	 * @param board - the board to be copied
	 * @return the copy of board
	 */
	public int[][] copyBoard(int[][] board) {
		int[][] newboard = new int[9][9];
		for (int row=0; row<9; row++) {
			for (int col=0; col<9; col++) {
				newboard[row][col] = board[row][col];
			}
		}
		return newboard;
	}
	
	/**
	 * copies the supplied domain 
	 *
	 * @param domain - the domain to be copied
	 * @return the copy of domain
	 */
	public boolean[][][] copyDomain(boolean[][][] domain) {
		boolean[][][] newdomain = new boolean[9][9][10];
		for (int row=0; row<9; row++) {
			for (int col=0; col<9; col++) {
				for (int d=0; d<10; d++) {
					newdomain[row][col][d] = domain[row][col][d];
				}
				
			}
		}
		return newdomain;
	}
}