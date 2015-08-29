import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;
import java.awt.Dimension;

public class Draw extends JPanel {

    private Matrice matrice;
    private static final int SIZE_PIECE = 16;
    
    public void paint(Graphics g)
    {
		int x,y;
        super.paint(g);
		for (y=0;y<20;y++) {
			for (x=0;x<10;x++) {
				if (matrice.get(x,y)) {
					g.setColor(matrice.getCol(x,y));
				} else {
					g.setColor(Color.white);
				}
				g.fillRect(x<<4, y<<4, SIZE_PIECE, SIZE_PIECE);
			}
		}
		
		// draw grid
		for (y=1;y<20;y++)
		{
			g.setColor(Color.gray);
			g.drawLine(0, y*SIZE_PIECE, g.getClipBounds().width, y*SIZE_PIECE);
			
			for (x=1;x<10;x++)
			{
				g.setColor(Color.gray);
				g.drawLine(x*SIZE_PIECE, 0, x*SIZE_PIECE, g.getClipBounds().height);
			}
		}

    }
    
    public void refresh()
    {
		matrice.refresh();
		repaint(getVisibleRect());
    }

    public Draw(Matrice m)
    {
		super();
		setPreferredSize(new Dimension(SIZE_PIECE*10, SIZE_PIECE*20));
		matrice = m;
    }
}
