import java.rmi.Remote; 
import java.rmi.RemoteException; 

public interface PieceInterface extends Remote {
    public int[][] get_piece_coord() throws RemoteException;
    public int get_piece_center() throws RemoteException;
}
