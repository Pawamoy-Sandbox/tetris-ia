import java.awt.Color;

public class LPiece extends Piece
{
	public LPiece()
	{
		super();
		int i = (center+1)%4;
		coord[i][0]=coord[center][0];
		coord[i][1]=coord[center][1]-1;
		i = (i+1)%4;
		coord[i][0]=coord[center][0];
		coord[i][1]=coord[center][1]+1;
		i = (i+1)%4;
		coord[i][0]=coord[center][0]+1;
		coord[i][1]=coord[center][1]+1;
		color = Color.cyan;
	}
	
	public LPiece(int coordinate[][])
	{
		super(coordinate);
		color = Color.cyan;
	}
}
