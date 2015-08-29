// Robot
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
// Client
import java.rmi.* ; 
import java.net.MalformedURLException ; 
import java.util.*;
// Lecture fichier
import java.io.*;
// String, char
import java.lang.String;

public class IA
{
    static Robot robot;
    static MatriceInterface matrice;
    static PieceInterface piece;
    static int[][] cur_piece;
    static int center;
    static int autodelay = 80;
    static float[] weight;
    
    static void DeleteLine(boolean[][] t, int l)
    {
		int x,y;
		for (y=l; y>0; y--)
			for (x=0; x<10; x++)
				t[x][y] = t[x][y-1];
				
		for (x=0; x<10; x++)
			if (t[x][0] != false)
				t[x][0] = false;
	}
    
    static int CountLines(boolean[][] t)
    {
		int x,y;
		int blocks_in_a_line = 0;
		int number_lines = 0;
		int iteration = 0;
		int[] lines = new int[4];
		for (x=0; x<4; x++)
			lines[x]=-1;
		
		for (y=19;y>=0;y--) 
		{
			blocks_in_a_line = 0;
			
			for (x=0;x<10;x++)
			{
				if (t[x][y] == true) 
				{
					blocks_in_a_line++;
				} 
				
				else 
				{
					break;
				}		
			}
			
			if (blocks_in_a_line == 10)
			{
				number_lines++;
				lines[number_lines-1] = y;
				if (number_lines == 1)
					iteration=1;
			}
			
			if (iteration > 0)
				iteration++;
			
			if (iteration == 4)
				break;
		}
		
		if (number_lines > 0)
		{
			for (x=3; x>=0; x--)
				if (lines[x] != -1)
					DeleteLine(t, lines[x]);
		}
		
		return number_lines;
	}
	
    static int HowHigh(boolean[][] t)
    {
		int x,y;
		int current_height = 0;
		
		for (y=19;y>=0;y--) 
		{
			for (x=0;x<10;x++)
			{
				if (t[x][y] == true) 
				{
					current_height = y;
					break;
				} 
			}
		}
		
		return 20 - current_height;
	}
	
    static int CountHoles(boolean[][] t)
    {
		int x,y;
		int number_of_holes = 0;
		int number_of_holes_in_column = 0;
		
		for (x=0;x<10;x++)
		{
			for (y=19;y>=0;y--) 
			{
				// we have no block at this position
				if (t[x][y] == false) 
				{
					// we initialize holes in the column at 0
					number_of_holes_in_column = 0;			
					// so we check pieces above
					// until we hit a block
					do
					{
						number_of_holes_in_column++;
						y--;
						
						if (y < 0)
						{
							// we got out of bounds, it means that there
							// is no filled space above the previous holes,
							// hence they can't be considered as holes
							number_of_holes_in_column = 0;
							// we make sure we don't go out of bounds
							break;
						}
						
					} while (t[x][y] != true);
					
					number_of_holes += number_of_holes_in_column;
				} 
			}
		}
		
		return number_of_holes;
	}
	
    static int CountBlockages(boolean[][] t)
    {
		int x,y;
		int number_of_blockages = 0;
		int number_of_blockages_in_column = 0;
		
		for (x=0;x<10;x++)
		{
			for (y=0;y<20;y++) 
			{
				// we have a block at this position
				if (t[x][y] == true) 
				{					
					// so we check pieces below
					// until we hit a hole 
					do
					{
						number_of_blockages_in_column++;
						y++;
						
						if (y > 19)
						{
							// we got out of bounds, it means that there
							// is no hole below the previous blocks,
							// hence they can't be considered as blockages
							number_of_blockages_in_column = 0;
							// we make sure we don't go out of bounds
							break;
						}
						
					} while (t[x][y] != false);
					
					number_of_blockages += number_of_blockages_in_column;
				} 
			}
		}
		
		return number_of_blockages;
	}
	
    static int Bumpiness(boolean[][] t)
    {
		int x,y;
		int bumpiness = 0;
		int previous_height = 20;
		int current_height;
		
		// first column
		// we get height
		for (y=0;y<20;y++)
		{
			if (t[0][y] == false) 
			{
				previous_height--;
			}
			else
			{
				break;
			}
		}
		
		// other columns
		for (x=1;x<10;x++) 
		{
			// init height
			current_height = 20;
			// we get height
			for (y=0;y<20;y++)
			{
				if (t[x][y] == false) 
				{
					current_height--;
				}
				else
				{
					break;
				}
			}
			
			// we compare it to the previous height
			if (current_height > previous_height)
			{
				bumpiness += current_height - previous_height;
			}
			else if (current_height < previous_height)
			{
				bumpiness += previous_height - current_height;
			}
			
			// we store current_height in previous_height
			previous_height = current_height;
		}
		
		return bumpiness;
	}	
	
	static int CountWells(boolean[][] t)
	{
		int x,y;
		int number_of_wells = 0;
		
		for (x=0; x<10; x++)
		{				
			for (y=3; y<20; y++) // no need to check higher, can't find wells before y=3
			{
				if (t[x][y] == true || y == 19)
				{
					if (x == 0)
					{
						// beside left edge
						if (t[x+1][y-3] == true)
						{
							number_of_wells++;
						}
					}
					else if (x == 9)
					{
						// beside right edge
						if (t[x-1][y-3] == true)
						{
							number_of_wells++;
						}
					}
					else
					{
						// not beside edge
						if (t[x-1][y-3] == true && t[x+1][y-3] == true)
						{
							number_of_wells++;
						}
					}
					// block spotted, no need to continue
					break;
				}
			}
		}
		
		return number_of_wells;
	}
	
	static void GetAutoDelay()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("ia/autodelay");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read one line of file
			if ((strLine = br.readLine()) != null)
			{
				autodelay = (int)Integer.valueOf(strLine);
			}
			// Close the input stream
			in.close();
		}
		catch (Exception e)
		{
			// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	static void GetWeights()
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream("ia/weights");
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			String[] fields; // = new String[6];
            int i = 0;
			// Read one line of file
			while ((strLine = br.readLine()) != null)
			{
				fields = strLine.split("=");
                Log.getInstance().Write("fields: " + fields[0] + " = " + fields[1]);
				
                weight[i] = (float)Float.valueOf(fields[1]);
                i++;
			}
			// Close the input stream
			in.close();
		}
		catch (Exception e)
		{
			// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		
		/**debug */
	}
	
	static float Score(boolean[][] t)
	{		
		// il faut séparer le comptage des lignes
		// voir PutInBestPlace
		
		//~ int lines = CountLines(t);
		int height = HowHigh(t);
		int holes = CountHoles(t);
		int blockages = CountBlockages(t);
		int bumpiness = Bumpiness(t);
		int wells = CountWells(t);
		
		float score = 0;
		//~ score += (lines * weight[0]);
		score += (height * weight[1]);
		score += (holes * weight[2]);
		score += (blockages * weight[3]);
		score += (bumpiness * weight[4]);
		score += (wells * weight[5]);
		
		return score;
	}
	
	static void CopyGrid(boolean[][] grid, boolean[][] grid_copy)
	{
		for (int i=0; i<10; i++)
			for (int j=0; j<20; j++)
				grid_copy[i][j] = grid[i][j];
	}
	
	static void CopyCoord(int[][] coord, int[][] coord_copy)
	{
		for (int i=0; i<4; i++)
			for (int j=0; j<2; j++)
				coord_copy[i][j] = coord[i][j];
	}
	
	static void ControlCoord(int[][] coord)
	{
		boolean move_right = false;
		boolean move_up = false;
		boolean move_left = false;
		boolean move_down = false;
		
		for (int i=0; i<4; i++)
		{
			if (coord[i][0]<0) move_right = true;
			if (coord[i][0]>9) move_left = true;
			if (coord[i][1]<0) move_down = true;
			if (coord[i][1]>19) move_up = true;
		}
		
		for (int i=0; i<4; i++)
		{
			if (move_right)
				coord[i][0]++;
			if (move_left)
				coord[i][0]--;
			if (move_up)
				coord[i][1]--;
			if (move_down)
				coord[i][1]++;
		}
	}
	
	static void Rotate(int[][] coord, int center)
	{
		int ecart[] = new int[2];
		
		if (center != -1)
		{
			// center==-1 <=> square
			for (int i=0; i<4; i++)
			{
				if (i==center) continue;
				ecart[0] = coord[i][0]-coord[center][0];
				ecart[1] = coord[i][1]-coord[center][1];
				
				coord[i][1] = coord[center][1] + ecart[0];
				coord[i][0] = coord[center][0] - ecart[1];
			}
			
			ControlCoord(coord);
		}
	}				
	
	static void Fall(boolean mat[][], int[][] coord)
	{
		while
		(
			(coord[0][1] < 19) &&
			(coord[1][1] < 19) &&
			(coord[2][1] < 19) &&
			(coord[3][1] < 19) &&
			(mat[coord[0][0]][coord[0][1]+1] == false) &&
			(mat[coord[1][0]][coord[1][1]+1] == false) &&
			(mat[coord[2][0]][coord[2][1]+1] == false) &&
			(mat[coord[3][0]][coord[3][1]+1] == false)
		)
		{
			for (int i=0; i<4; i++)
				coord[i][1]++;
		}
	}
	
	static int PasteOnLeftEdge(boolean mat[][], int[][] coord)
	{
		int how_many_times = 0;
		while
		(
			(coord[0][0] > 0) &&
			(coord[1][0] > 0) &&
			(coord[2][0] > 0) &&
			(coord[3][0] > 0) &&
			(mat[coord[0][0]-1][coord[0][1]] == false) &&
			(mat[coord[1][0]-1][coord[1][1]] == false) &&
			(mat[coord[2][0]-1][coord[2][1]] == false) &&
			(mat[coord[3][0]-1][coord[3][1]] == false)
		)
		{
			for (int i=0; i<4; i++)
				coord[i][0]--;
				
			how_many_times++;
		}
		return how_many_times;
	}
	
	static boolean MoveRight(boolean mat[][], int[][] coord)
	{
		boolean moved = false;
		if
		(
			(coord[0][0] < 9) &&
			(coord[1][0] < 9) &&
			(coord[2][0] < 9) &&
			(coord[3][0] < 9) &&
			(mat[coord[0][0]+1][coord[0][1]] == false) &&
			(mat[coord[1][0]+1][coord[1][1]] == false) &&
			(mat[coord[2][0]+1][coord[2][1]] == false) &&
			(mat[coord[3][0]+1][coord[3][1]] == false)
		)
		{
			for (int i=0; i<4; i++)
				coord[i][0]++;
				
			moved = true;
		}
		//~ if (moved == false)
		//~ {
			//~ System.out.println("PIECE COORDINATES:");
			//~ for (int i=0; i<4; i++)
				//~ System.out.println("["+coord[i][0]+"]["+coord[i][1]+"]");
			//~ System.out.println("BEFORE INSERTING PIECE:");
			//~ display_matrice(mat);
			//~ InsertPiece(mat, coord, true);
			//~ System.out.println("AFTER INSERTED PIECE:");
			//~ display_matrice(mat);
			//~ InsertPiece(mat, coord, false);
			//~ System.out.println("AFTER RETRIEVED PIECE:");
			//~ display_matrice(mat);
		//~ }
		return moved;
	}
	
	static void InsertPiece(boolean mat[][], int[][] coord, boolean insert)
	{
		for (int i=0; i<4; i++)
			mat[coord[i][0]][coord[i][1]] = insert;
	}
	
	static boolean Collide(boolean mat[][], int[][] coord)
	{
		boolean collide = false;
		for (int i=0; i<4; i++)
		{
			if (mat[coord[i][0]][coord[i][1]] == true)
			{
				collide = true;
			}
		}
		return collide;
	}
	
	//~ static void ShowMatrixWithPiece(boolean mat[][], int[][] coord)
	//~ {
		//~ display_matrice(mat);
		//~ InsertPiece(mat, coord, true);
		//~ display_matrice(mat);
		//~ InsertPiece(mat, coord, false);
	//~ }
	
	static void PutInBestPlace(boolean[][] g, int[][] p, int center)
	{
		boolean g_clone[][] = new boolean[10][20];
		boolean g_clone2[][] = new boolean[10][20];
		int[][] p_clone = new int[4][2];
		int[][] coord_backup = new int[4][2];
		// 4 rotations, 10 positions (max), + gap between current positin and left edge
		float[][] score_tab = new float[4][11];
		int i,j,k,l;
		boolean k_break;
		float score;
		float best_score = 10000;
		int best_rotation = 0;
		int best_move = 0;
		char direction;
		int key;
		
		for (i=0; i<4; i++)
			for (j=0; j<10; j++)
				score_tab[i][j] = best_score;
				
		CopyGrid(g, g_clone);
		CopyCoord(p, p_clone);
		
		/** start calculating scores for each possible position */
		// first we retrieve piece coordinates of the matrix (like a clean-up)
		InsertPiece(g_clone, p_clone, false);
		// backup matrix
		CopyGrid(g_clone, g_clone2);
		
		// for each rotation
		for (i=0; i<4; i++)
		{
			//~ System.out.println("Rotation "+i);
			// restore piece first position
			CopyCoord(p, p_clone);
			
			// rotate i times
			for (j=0; j<i; j++)
				Rotate(p_clone, center);
				
			// save gap between this position and left edge
			score_tab[i][10] = PasteOnLeftEdge(g_clone, p_clone);
			
			// backup coord on left edge
			CopyCoord(p_clone, coord_backup);
			
			// for each position until we can't move again on the right
			for (k=0; k<10; k++)
			{
				k_break = false;
				// restore coord on left edge
				CopyCoord(coord_backup, p_clone);
				
				//~ System.out.print("Move Right "+k+" times...");
				// move on the right k times
				for (l=0; l<k; l++)
				{
					if (MoveRight(g_clone, p_clone) == false)
					{
						//~ System.out.println(" failure at "+l+"th");
						// we were not able to move enough
						// no need to continue for this rotation
						k_break = true;
						break;
					}
					//~ System.out.println(" success");
				}
				
				// continue to next rotation
				if (k_break == true)
					break;
					
				// we were able to move
				//~ System.out.println("Fall");
				Fall(g_clone, p_clone);
				// is there is a collision, we set a very bad score
				if (Collide(g_clone, p_clone) == true)
				{
					//~ System.out.println("Collide, stop and continue with next position");
					score_tab[i][k] = best_score;
					continue;
				}
				// we can insert coordinates of the piece
				// into the matrix, and calculate a score
				//~ ShowMatrixWithPiece(g_clone, p_clone);
				InsertPiece(g_clone, p_clone, true);
				// we count the lines completed (and delete them, side effect)
				score = weight[0] * CountLines(g_clone);
				//~ display_matrice(g_clone);
				// we calculte the other criterias
				score += Score(g_clone);
				score_tab[i][k] = score;
				// then we restore the matrix
				CopyGrid(g_clone2, g_clone);
			}
			
			if (center == -1)
				break;
		}
		
		/** now we can pick out the best score, what let us know
		 * how many times to rotate and how many times to move on
		 * the right from the left edge */
		for (i=0; i<4; i++)
		{
			for (j=0; j<10; j++)
			{
				if (score_tab[i][j] < best_score)
				{
					best_score = score_tab[i][j];
					best_rotation = i;
					best_move = j;
				}
			}
		}
		 
		best_move -= score_tab[best_rotation][10];
		 
		/** the last thing to do is to send keys via the robot */
		/** debug */
		//~ System.out.print(best_rotation+" rotations, and ");
		
		for (i=0; i<best_rotation; i++)
			send_key(2);
		
		if (best_move < 0)
		{
			key = 1;
			best_move = -(best_move);
		}
		else
		{
			key = 0;
		}
			
		for (i=0; i<best_move; i++)
			send_key(key);
			
		send_key(3);
	}
	
	static void SetPiece(int[][] coord, int c)
	{
		if (coord == null)
			System.exit(0);
			
		CopyCoord(coord, cur_piece);
		center = c;
	}
	
	static void BeANearPerfectPlayer()
	{
		while (true)
		{
			get_piece();
			PutInBestPlace(get_matrice(), cur_piece, center);
		}
	}
	
    static void my_sleep(int ms)
    {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
    }
    
    static public void run(String exec)
    {
		try {
			Runtime.getRuntime().exec(exec);
			//~ System.out.println("IA : Wait running "+exec);
			my_sleep(1000);
		} catch (IOException ex) {
			Logger.getLogger(IA.class.getName()).log(Level.SEVERE, null, ex);
		}
    }
    
    public static void send_key(int key) 
    {
		int k=0;
		switch (key) {
			case 0:k = KeyEvent.VK_RIGHT;	break; 	// 0:Droite
			case 1:k = KeyEvent.VK_LEFT; 	break;	// 1:Gauche
			case 2:k = KeyEvent.VK_UP;		break;	// 2:Rotation
			case 3:k = KeyEvent.VK_SPACE; 	break;	// 3:Tomber
		};
		robot.keyPress(k);
		robot.keyRelease(k);      
    }

    public static void display_matrice(boolean t[][])
    {   
		int x,y;
		//~ System.out.println("Matrice vue par l'IA : ");
		for (y=0;y<20;y++) {
			for (x=0;x<10;x++) {
				if (t[x][y]) {
					System.out.print("[]");
				} else {
					System.out.print("--");
				}		
			}
			System.out.println("");
		}
    }

    public static boolean[][] get_matrice()
    {
		boolean [][] result=null;
		try
		{
			do
			{
				result = matrice.get_matrice();
				if (result == null)
					my_sleep(100);
			} while (result == null);
		} 
		catch (RemoteException re)
		{ 
			System.out.println(re) ;
		}
		return result;
    }
    
    public static void get_piece()
    {
		int[][] coord_result = null;
		int center_result = 0;
		
		try
		{
			do
			{
				coord_result = piece.get_piece_coord();
				center_result = piece.get_piece_center();
				if (coord_result == null)
					my_sleep(100);
			} while (coord_result == null);
		} 
		catch (RemoteException re)
		{ 
			//~ System.out.println(re) ;
		}
			
		SetPiece(coord_result, center_result);
    }
    
    public static void main(String[] args) throws AWTException, IOException
    {	
		// Execution 
		if (args.length < 1)
		{
			System.out.println("Please specify on which of demo or tetris to run IA");
			System.exit(1);
		}
		
		String cmd = new String();
		cmd = cmd.concat("java ");
		cmd = cmd.concat(args[0]);
		
		if (args[0].equals("demo"))
		{
			if (args.length < 2)
			{
				System.out.println("java demo needs at least one parameter (list of pieces)");
				System.exit(1);
			}
			cmd = cmd.concat(" ");
			cmd = cmd.concat(args[1]);
			if (args.length >= 3)
			{
				cmd = cmd.concat(" ");
				cmd = cmd.concat(args[2]);
			}
		}
		
		cur_piece = new int[4][2];
		weight = new float[6];
		for (int i=0; i<6; i++)
			weight[i] = 0;
		
		run(cmd);
		
		// Robot
		try {
			robot = new Robot();
			robot.setAutoWaitForIdle(false);
		} catch (AWTException ex) {
			Logger.getLogger(IA.class.getName()).log(Level.SEVERE, null, ex);
		}
		// Client
		try {
			matrice = (MatriceInterface)Naming.lookup("//localhost/matrice");
		} catch (MalformedURLException e) { System.out.println(e) ; }
		catch (NotBoundException re) { System.out.println(re) ; }
		try {
			piece = (PieceInterface)Naming.lookup("//localhost/piece");
		} catch (MalformedURLException e) { System.out.println(e) ; }
		catch (NotBoundException re) { System.out.println(re) ; }
		
		//~ System.out.println("I'll try to make a BIG score !");
		GetWeights();
		GetAutoDelay();
		robot.setAutoDelay(autodelay);
		BeANearPerfectPlayer();
    }
}
