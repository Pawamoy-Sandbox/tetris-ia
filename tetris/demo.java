// Connexion IA
import java.net.* ;
import java.rmi.* ;
// Graphique
import javax.swing.*;
// Clavier
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// Timer
import java.util.Timer;
import java.util.TimerTask;
// List
import java.util.List;
import java.util.ArrayList;
// Lecture fichier
import java.io.*;
// String, char
import java.lang.String;
// random
import java.util.Random;

class demo {

    static Matrice matrice;
    static Draw draw;

	static CurrentPiece current_piece;
	static ArrayList<Piece> pieces;
	
	static int points = 0;
	static int total_lines = 0;
	
	static String pieces_list;
	static int list_pos = 0;

	public static void GameOver()
	{
		// à compléter
		//~ JOptionPane.showMessageDialog(null, "Score final : " + String.valueOf(points), "GAME OVER", JOptionPane.PLAIN_MESSAGE);
		System.out.println(points+" "+total_lines);
		System.exit(points);
	}
	
	public static int randInt(int min, int max)
	{

		// Usually this can be a field rather than a method variable
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
	
	public static void RandNewPiece()
    {
		int random_number = randInt(1, 7);
		switch (random_number)
		{
			// I piece
			case 1:
				current_piece.p = new IPiece();
			break;
			
			// J piece
			case 2:
				current_piece.p = new JPiece();
			break;
			
			// L piece
			case 3:
				current_piece.p = new LPiece();
			break;
			
			// O piece
			case 4:
				current_piece.p = new OPiece();
			break;
			
			// S piece
			case 5:
				current_piece.p = new SPiece();
			break;
			
			// T piece
			case 6:
				current_piece.p = new TPiece();
			break;
			
			// Z piece
			case 7:
				current_piece.p = new ZPiece();
			break;
		}
    }
	
	public static void NextPiece()
    {
		boolean check_collision = false;
		char letter;
		// From top to bottom: I, J, L, O, S, T, Z. 
		
		// if the current piece actually exists
		if (current_piece.p != null)
		{
			pieces.add(current_piece.p);
			current_piece.p = null;
			check_collision = true;
		}
		
		if (list_pos == pieces_list.length())
		{
			RandNewPiece();
		}
		else
		{
			letter = pieces_list.charAt(list_pos);
			list_pos++;
		
			switch (Character.toLowerCase(letter))
			{
				// I piece
				case 'i':
					current_piece.p = new IPiece();
				break;
				
				// J piece
				case 'j':
					current_piece.p = new JPiece();
				break;
				
				// L piece
				case 'l':
					current_piece.p = new LPiece();
				break;
				
				// O piece
				case 'o':
					current_piece.p = new OPiece();
				break;
				
				// S piece
				case 's':
					current_piece.p = new SPiece();
				break;
				
				// T piece
				case 't':
					current_piece.p = new TPiece();
				break;
				
				// Z piece
				case 'z':
					current_piece.p = new ZPiece();
				break;
				
				// wrong number
				default:
					current_piece.p = new IPiece();
				break;
			}
		}
		
		if (check_collision)
			if (CheckCollision())
				GameOver();
    }
	
	public static void action(String s)
	{
		boolean moved;
		current_piece.p.BackupCoord();
		current_piece.p.Draw(matrice, draw, false);
		if (s.compareToIgnoreCase("right")==0)
		{
			moved = current_piece.p.MoveRight();
			if (moved)
				if (CheckCollision())
					current_piece.p.RestoreCoord();
		}
		else if (s.compareToIgnoreCase("left")==0)
		{
			moved = current_piece.p.MoveLeft();
			if (moved)
				if (CheckCollision())
					current_piece.p.RestoreCoord();
		}
		else if (s.compareToIgnoreCase("down")==0)
		{
			moved = current_piece.p.MoveDown();
			if (moved)
			{
				if (CheckCollision())
				{
					current_piece.p.RestoreCoord();
					current_piece.p.SetStuck();
				}
			}
			else
				current_piece.p.SetStuck();
		}
		else if (s.compareToIgnoreCase("rotate")==0)
		{
			current_piece.p.Rotate();
			if (CheckCollision())
				current_piece.p.RestoreCoord();
		}
		else if (s.compareToIgnoreCase("fall")==0)
		{
			current_piece.p.Fall(matrice.matriceB);
		}
		current_piece.p.Draw(matrice, draw, true);
		draw.refresh();
	}
	
	public static int CheckLine()
	{
    	int line_count = 0;
    	int iter_count = 0;
    	
    	// stockage des lignes complétées
    	int lines[] = new int[4];
    	for (int i=0; i<4; i++)
    		lines[i]=-1;
    	
    	// vérification des lignes
    	for (int j=19; j>=0; j--)
    	{
    		if (matrice.LineCompleted(j))
    		{
    			lines[line_count]=j;
    			line_count++;
    		}
    		
    		if (line_count>0)
    			iter_count++;
    		// pas besoin de vérifier + de 4 lignes une fois qu'on en a trouvé une
    		if (iter_count==4) 
    			break;
    	}
    	
    	// total des lignes complétées
    	total_lines += line_count;
    	
    	// pour chaque ligne complétée (en partant de la plus haute)
    	for (int i=3; i>=0; i--)
    	{
    		if (lines[i]!=-1)
    		{
    			// on actualise la matrice
    			matrice.LinesDown(lines[i]);
    			
    			// on actualise les pièces
    			for (int j=0; j<pieces.size(); j++)
    				pieces.get(j).Draw(matrice, draw, false);
    				
    			draw.refresh();
    			
    			for (int j=0; j<pieces.size(); j++)
    				pieces.get(j).PieceDown(lines[i]);
    				
    			for (int j=0; j<pieces.size(); j++)
    				pieces.get(j).Draw(matrice, draw, true);
    				
    			draw.refresh();
    		}
    	}
    	
    	// on renvoie les points gagnés
    	switch(line_count)
    	{
    		case 1:
    			return 40;
    		case 2:
    			return 100;
    		case 3:
    			return 300;
    		case 4:
    			return 1200;
    		default:
    			return 0;
    	}
	}
	
	public static void CheckStuck()
	{
		int gain;
		if (current_piece.p.IsStuck() == true)
		{
			// on ajoute la pièce courante dans le tableau et on génère la nouvelle
			NextPiece();
			// une ligne supprimée peut conduire à d'autres lignes complétées
			while ((gain = CheckLine()) > 0)
				points += gain;
			
			current_piece.p.Draw(matrice, draw, true);
			draw.refresh();
		}
	}
    
    public static boolean CheckCollision()
    {
		for (int i = 0; i < pieces.size(); i++)
		{
			if (current_piece.p.IsCollidedWith(pieces.get(i)) == true)
				return true;
		}
		return false;
	}

	public static void GetStuckPieces(String filename)
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			String[] fields; // = new String[5];
			String[] axes; // = new String[2];
			char piece_letter;
			int coordinate[][] = new int[4][2];
			Piece piece;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null)
			{
				fields = strLine.split(" ");
				
				piece_letter = fields[0].charAt(0);
				
				for (int i=1; i<5; i++)
				{
					axes = fields[i].split(":");
					coordinate[i-1][0] = (int)Integer.valueOf(axes[0]);
					coordinate[i-1][1] = (int)Integer.valueOf(axes[1]);
				}
					
				switch (Character.toLowerCase(piece_letter))
				{
					case 'i': piece = new IPiece(coordinate); break;
					case 'j': piece = new JPiece(coordinate); break;
					case 'l': piece = new LPiece(coordinate); break;
					case 'o': piece = new OPiece(coordinate); break;
					case 's': piece = new SPiece(coordinate); break;
					case 't': piece = new TPiece(coordinate); break;
					case 'z': piece = new ZPiece(coordinate); break;
					default : piece = new IPiece(coordinate); break;
				}
				
				piece.Draw(matrice, draw, true);
				pieces.add(piece);
			}
			// Close the input stream
			in.close();
		}
		catch (Exception e)
		{
			// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
		draw.refresh();
	}

	public static void GetNextPiecesList(String filename)
	{
		try
		{
			// Open the file that is the first 
			// command line parameter
			FileInputStream fstream = new FileInputStream(filename);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null)
			{
				pieces_list = pieces_list.concat(strLine);
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
			
    public static void main(String[] args)
    {
		// Connexion IA
		try {
			current_piece = new CurrentPiece();
			Naming.rebind("piece",current_piece);
		} catch (RemoteException re) {
			System.out.println(re);
		} catch (MalformedURLException e) {
			System.out.println(e);
		}
		
		try {
			matrice = new Matrice();
			Naming.rebind("matrice",matrice) ;
		} catch (RemoteException re) { System.out.println(re) ; }
		  catch (MalformedURLException e) { System.out.println(e) ; }

		// Base Graphique
		final JFrame f = new JFrame("TetriS");
		draw = new Draw(matrice);
		f.getContentPane().add(draw);	
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.pack();
		f.setVisible(true);

		// Clavier
		f.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_RIGHT: action("right");  break;
					case KeyEvent.VK_LEFT:  action("left");   break;
					case KeyEvent.VK_UP:    action("rotate"); break;
					case KeyEvent.VK_DOWN:	action("down");   break;
					case KeyEvent.VK_SPACE: action("fall");   break;
				}
				CheckStuck();
			}
			public void keyReleased(KeyEvent e) {
				// KEY RELEASED
			}		
		});
		System.out.println("Début du jeu...");

		pieces = new ArrayList<Piece>();
		pieces_list = new String();
		
		// traitement des arguments sur la ligne de commande
		if (args.length >= 1)
			GetNextPiecesList(args[0]);
		else
		{
			System.out.println("usage: java demo LIST [PIECES]");
			System.exit(1);
		}
		if (args.length == 2)
			GetStuckPieces(args[1]);
			
		// première pièce 
		NextPiece();
		
		/** debug IA */
		//~ System.out.println("");
		// décommenter dans la fonction Score pour afficher les valeurs
		//~ IA.Score(matrice.matriceB);
			
		current_piece.p.Draw(matrice, draw, true);
		draw.refresh();
		
		// init timer
		Timer timer_fall = new Timer();
		Timer timer_stuck = new Timer();

		timer_fall.scheduleAtFixedRate(
		new TimerTask() 
		{
			@Override
			public void run() 
			{
				action("down");
				CheckStuck();
				f.setTitle("TetriS " + String.valueOf(points));
			}
		},
		1*1000,
		1*1000);
    }
}
