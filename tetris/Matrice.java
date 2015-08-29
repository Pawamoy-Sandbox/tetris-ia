import java.rmi.server.UnicastRemoteObject ;
import java.rmi.RemoteException ;
import java.awt.Color;

class Matrice extends UnicastRemoteObject implements MatriceInterface {

    protected boolean matrice_IA[][];
    protected boolean matriceB[][];
    protected Color matriceC[][];
    private boolean is_busy;

    public Matrice() throws RemoteException 
    {
		super();
		matriceB = new boolean[10][20];
		matriceC = new Color[10][20];
		matrice_IA = new boolean[10][20];
		for (int i=0; i<10; i++)
			for (int j=0; j<20; j++)
			{
				matriceB[i][j] = false;
				matriceC[i][j] = Color.white;
				matrice_IA[i][j] = false;
			}
    };

    // Appel distant pour l'IA
    public boolean[][] get_matrice() throws RemoteException {	
		if (is_busy) return null;
		return matrice_IA;
    }
    // Fin de l'appel distant.

    public void refresh()
    {
		int x,y;
		is_busy = true;
		for (y=0;y<20;y++)
			for (x=0;x<10;x++)
				matrice_IA[x][y] = matriceB[x][y];
		is_busy = false;
    }

    public void put(int x,int y,boolean v,Color c)
    {
		matriceB[x][y] = v;
		matriceC[x][y] = c;
    }

    public boolean get(int x,int y)
    {
		return matriceB[x][y];
    }
    
    public Color getCol(int x,int y)
    {
		return matriceC[x][y];
    }
    
    public boolean LineCompleted(int y)
    {
    	boolean complete = true;
		for (int x=0; x<10; x++)
		{
			if (get(x,y)==false)
			{
				complete = false;
				break;
			}
		}
		return complete;
	}
	
	public boolean LineEmpty(int y)
	{
		boolean empty = true;
		for (int x=0; x<10; x++)
		{
			if (get(x,y)==true)
			{
				empty = false;
				break;
			}
		}
		return empty;
	}
	
	public void ClearLine(int y)
	{
		for (int x=0; x<10; x++)
		{
			matriceB[x][y] = false;
		}
	}
    
    public void LinesDown(int y)
    {
    	for (int cur_line=y; cur_line>0; cur_line--)
    		for (int x=0; x<9; x++)
					put(x,cur_line, get(x, cur_line-1), getCol(x, cur_line-1));
    }
}
