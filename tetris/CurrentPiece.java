import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

class CurrentPiece extends UnicastRemoteObject implements PieceInterface
{
	public Piece p;
	
	public CurrentPiece() throws RemoteException 
	{
		p = null;
	}
	
	// Appel distant pour l'IA
    public int[][] get_piece_coord() throws RemoteException
    {
		return p.coord;
	}
	public int get_piece_center() throws RemoteException
    {
		return p.center;
	}
	// Fin de l'appel distant
}
