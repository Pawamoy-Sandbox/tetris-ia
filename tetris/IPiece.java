import java.awt.Color;

public class IPiece extends Piece
{
	public IPiece()
	{
		super();
		int i = (center+1)%4;
		coord[i][0]=coord[center][0]-1;
		coord[i][1]=coord[center][1];
		i = (i+1)%4;
		coord[i][0]=coord[center][0]+1;
		coord[i][1]=coord[center][1];
		i = (i+1)%4;
		coord[i][0]=coord[center][0]+2;
		coord[i][1]=coord[center][1];
		color = Color.yellow;
		MoveUp();
	}
	
	public IPiece(int coordinate[][])
	{
		super(coordinate);
		color = Color.yellow;
	}
}
