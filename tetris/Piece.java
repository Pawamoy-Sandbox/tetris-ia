import java.awt.Color;

public abstract class Piece
{
	// [case1,case2,case3,case4] [abscisse,ordonnee] 
	protected int coord[][];
	// sauvegarde temporaire de la position de la pièce
	protected int backup_coord[][];
	// indice de la case sur laquelle axer la rotation
	protected int center;
	// pièce posée ou en chute
	protected boolean stuck;
	// couleur de la pièce
	protected Color color;

	public Piece()
	{
		coord = new int[4][2];
		backup_coord = new int[4][2];
		for (int i=0; i<4; i++)
			for (int j=0; j<2; j++)
				coord[i][j] = 0;
		stuck = false;
		color = Color.black;
		center = 0;
		coord[center][0] = 4;
		coord[center][1] = 1;
	}
	
	public Piece(int coordinate[][])
	{
		coord = new int[4][2];
		backup_coord = new int[4][2];
		for (int i=0; i<4; i++)
			for (int j=0; j<2; j++)
				coord[i][j] = coordinate[i][j];
		stuck = true;
		color = Color.black;
		center = 0;
	}
	
	public void BackupCoord()
	{
		for (int i=0; i<4; i++)
			for (int j=0; j<2; j++)
				backup_coord[i][j] = coord[i][j];
	}
	
	public void RestoreCoord()
	{
		for (int i=0; i<4; i++)
			for (int j=0; j<2; j++)
				coord[i][j] = backup_coord[i][j];
	}
	
	public boolean Move(int axe, int move, int limit)
	{
		int tmp[] = new int[4];
		// vérification temporaire
		for (int i=0; i<4; i++)
		{
			tmp[i] = coord[i][axe]+move;
			if (move>0){
				if (tmp[i]>limit) return false;
			}
			else {
				if (tmp[i]<limit) return false;
			}
		}
		// affectation des nouvelles valeurs
		for (int i=0; i<4; i++)
			coord[i][axe] = tmp[i];
		
		return true;
	}
		
	public boolean MoveLeft()  { return Move(0, -1, 0); }
	public boolean MoveRight() { return Move(0, +1, 9); }
	public boolean MoveUp()    { return Move(1, -1, 0); }
	public boolean MoveDown()  { return Move(1, +1, 19);}
	
	//~ public boolean MoveUpLeft()
	//~ { 
		//~ boolean m1 = MoveLeft();
		//~ boolean m2 = MoveUp();
		//~ return (m1 && m2);
	//~ }
	//~ public boolean MoveUpRight()
	//~ {
		//~ boolean m1 = MoveRight();
		//~ boolean m2 = MoveUp();
		//~ return (m1 && m2);
	//~ }
	//~ public boolean MoveDownRight()
	//~ {
		//~ boolean m1 = MoveRight();
		//~ boolean m2 = MoveDown();
		//~ return (m1 && m2);
	//~ }
	//~ public boolean MoveDownLeft()
	//~ {
		//~ boolean m1 = MoveLeft();
		//~ boolean m2 = MoveDown();
		//~ return (m1 && m2);
	//~ }
	
	public void Fall(boolean mat[][])
	{
		// tant que les 4 cases dans la matrice
		// sous les 4 cases de la pièce sont vides
		// on fait descendre la pièce
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
			MoveDown();
		}
		SetStuck();
	}
	
	public boolean IsStuck() { return stuck; }
	public void SetStuck()   { stuck = true; }
	
	public void ControlCoord()
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
	
	public void Rotate()
	{
		int ecart[] = new int[2];
		for (int i=0; i<4; i++)
		{
			if (i==center) continue;
			ecart[0] = coord[i][0]-coord[center][0];
			ecart[1] = coord[i][1]-coord[center][1];
			
			coord[i][1] = coord[center][1] + ecart[0];
			coord[i][0] = coord[center][0] - ecart[1];
		}
		
		ControlCoord();
	}

	// disloquation des pièces, chute des cases :
	// tant que place libre en dessous, faire tomber la case
	public boolean PieceDown(int y)
	{
		boolean redraw = false;
		for (int i=0; i<4; i++)
		{
			if (coord[i][1]==y)
			{
				redraw = true;
				coord[i][0]=10;
				coord[i][1]=20;
			}
			else if (coord[i][1]<y)
			{
				redraw = true;
				coord[i][1]++;
			}
		}
		
		return redraw;
	}
	
	public boolean IsCollidedWith(Piece p)
	{
		for (int i=0; i<4; i++)
			for (int j=0; j<4; j++)
				if (
					this.coord[i][0] == p.coord[j][0] &&
					this.coord[i][1] == p.coord[j][1]
				   )
				{
					/** debug */
					//~ System.out.print("Collision true between "+this+" and "+p);
					//~ System.out.println(" (case "+i+","+j+":["+p.coord[j][0]+"]["+p.coord[j][1]+"])");
					return true;
				}
		return false;
	}
	
	public void Draw(Matrice matrice, Draw draw, boolean remove)
	{
		for (int i=0; i<4; i++)
			if (coord[i][0]<10 && coord[i][1]<20)
				matrice.put(coord[i][0],coord[i][1],remove,color);
		// on évite de faire un refresh pour chaque Draw...
		//~ draw.refresh();
	}
	
	public boolean Cleared()
	{
		int i;
		boolean cleared = true;
		for (i=0; i<4; i++)
		{
			if (coord[i][0] != 10 || coord[i][1] != 20)
				cleared = false;
		}
		return cleared;
	}
	
	/** debug */
	//~ public void printCoordinate()
	//~ {
		//~ for (int i=0; i<4; i++)
		//~ {
			//~ if (coord[i][0]!=10 && coord[i][1]!=20)
				//~ System.out.print("["+coord[i][0]+"]["+coord[i][1]+"] ");
			//~ else
				//~ System.out.print("[X][X] ");
		//~ }
	//~ }
}
