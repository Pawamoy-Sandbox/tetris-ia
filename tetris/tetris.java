// Connexion IA

import java.net.*;
import java.rmi.*;
// Graphique
import javax.swing.*;
import java.awt.GridBagLayout;      //ajouté par val
import java.awt.BorderLayout;       //ajouté par val
import java.awt.GridBagConstraints;
import java.awt.Dimension;          //ajouté par val
// Clavier
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
// Timer
import java.util.Timer;
import java.util.TimerTask;
// List
import java.util.List;
import java.util.ArrayList;
// Random
import java.util.Random;
// Write score in file
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/* IA (http://codemyroad.wordpress.com/2013/04/14/tetris-ai-the-near-perfect-player/)  
 * 
 * Fonction de calcul du score de toutes les positions:
 * 		CORRIGE (erreurs à la con...)
 *		Reste un cas à prendre en compte:
 * 			La rotation de la pièce aboutit sur une collision (actuellement non-traité par l'IA)
 * 			-> changer de technique:
 * 			-> var rotate_collide = true;
 * 			-> var moved_left = 0
 * 			-> tant que la position gauche extrême n'est pas atteinte
 * 				-> bouger d'un cran à gauche
 * 				-> moved_left++
 * 				-> rotation
 * 				-> si la rotation crée une collision, continue
 * 				-> sinon
 * 					-> var paste_move = paste on left edge
 * 					-> tester chaque position possible vers la droite (move++)
 * 					-> enregistrer les scores... (ainsi que move-=paste_move et moved_left)
 * 					-> break (tant que)
 * 				
 * 			-> réitérer du côté droit:
 * 			-> var moved_right = 0
 * 			-> tant que la position droite extrême n'est pas atteinte
 * 				-> bouger d'un cran à droite
 * 				-> moved_right++
 * 				-> rotation
 * 				-> si la rotation crée une collision, continue
 * 				-> sinon
 * 					-> (pas besoin de paste on left edge ici: var paste_move = 0)
 * 					-> tester chaque position possible vers la droite (move++)
 * 					-> enregistrer les scores... (ainsi que move et moved_right)
 * 					-> break (tant que)
 * 
 * 			-> envoi des touches:
 * 			-> si rotate_collide = true
 * 				-> d'abord bouger de moved_left/right à gauche/droite
 * 				-> effectuer une rotation
 * 				-> bouger de move (move<0 : gauche, move>0 : droite)
 * 				-> chuter
 */
class tetris {

    static Matrice matrice;
    static Draw draw;

    static CurrentPiece current_piece;
    static ArrayList<Piece> pieces;

    static int points = 0;
    static int total_lines = 0;

    public static void GameOver() {
		// à compléter
        //~ JOptionPane.showMessageDialog(null, "Score final : " + String.valueOf(points), "GAME OVER", JOptionPane.PLAIN_MESSAGE);
        System.out.println(points + " " + total_lines);
        Log.getInstance().Write("score :" + points);
        Log.getInstance().Write("total_lines :" + total_lines);
        System.exit(0);
    }

    public static int randInt(int min, int max) {

        // Usually this can be a field rather than a method variable
        Random rand = new Random();

		// nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    public static void RandNewPiece() {
        int random_number = randInt(1, 7);
        boolean check_collision = false;

		// From top to bottom: I, J, L, O, S, T, Z. 
        // if the current piece actually exists
        if (current_piece.p != null) {
            pieces.add(current_piece.p);
            current_piece.p = null;
            check_collision = true;
        }

        switch (random_number) {
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
        /**
         * debug
         */
		//~ System.out.println("Current piece : "+current_piece.p);

        if (check_collision) {
            if (CheckCollision()) // GAME OVER
            {
				current_piece.p.Draw(matrice, draw, true);
				draw.refresh();
                GameOver();
            }
        }
    }

    public static void action(String s) {
        boolean moved;
        current_piece.p.BackupCoord();
        current_piece.p.Draw(matrice, draw, false);
        if (s.compareToIgnoreCase("right") == 0) {
            moved = current_piece.p.MoveRight();
            if (moved) {
                if (CheckCollision()) {
                    current_piece.p.RestoreCoord();
                }
            }
        } else if (s.compareToIgnoreCase("left") == 0) {
            moved = current_piece.p.MoveLeft();
            if (moved) {
                if (CheckCollision()) {
                    current_piece.p.RestoreCoord();
                }
            }
        } else if (s.compareToIgnoreCase("down") == 0) {
            moved = current_piece.p.MoveDown();
            if (moved) {
                if (CheckCollision()) {
                    current_piece.p.RestoreCoord();
                    current_piece.p.SetStuck();
                }
            } else {
                current_piece.p.SetStuck();
            }
        } else if (s.compareToIgnoreCase("rotate") == 0) {
            current_piece.p.Rotate();
            if (CheckCollision()) {
                current_piece.p.RestoreCoord();
            }
        } else if (s.compareToIgnoreCase("fall") == 0) {
            current_piece.p.Fall(matrice.matriceB);
        }
        /**
         * debug
         */
		//~ System.out.print(s+"\t : ");
        //~ current_piece.p.printCoordinate();
        //~ System.out.println("");
        current_piece.p.Draw(matrice, draw, true);
        draw.refresh();
    }

    public static int CheckLine() {
        int line_count = 0;
        int iter_count = 0;
        int i,j;

        // stockage des lignes complétées
        int lines[] = new int[4];
        for (i = 0; i < 4; i++) {
            lines[i] = -1;
        }

        // vérification des lignes
        for (j = 19; j >= 0; j--) {
            if (matrice.LineCompleted(j)) {
                lines[line_count] = j;
                line_count++;
            }

            if (line_count > 0) {
                iter_count++;
            }
            // pas besoin de vérifier + de 4 lignes une fois qu'on en a trouvé une
            if (iter_count == 4) {
                break;
            }
        }

        // total des lignes complétées
        total_lines += line_count;
        
        // nettoyage si corruption matrice
        for (j=j; j>=0; j--) {
			if (matrice.LineEmpty(j)==true) {
				break;
			}
		}
		
		for (j=j-1; j>=0; j--) {
			matrice.ClearLine(j);
		}

        /**
         * debug
         */
    	//~ if (line_count>0)
        //~ {
        //~ System.out.println("SOME LINES ARE COMPLETED :");
        //~ IA.display_matrice(matrice.matrice);
        //~ }	
        // pour chaque ligne complétée (en partant de la plus haute)
        for (i = 3; i >= 0; i--) {
            if (lines[i] != -1) {
                // on actualise la matrice
                matrice.LinesDown(lines[i]);

                // on actualise les pièces
                for (j = 0; j < pieces.size(); j++) {
                    pieces.get(j).Draw(matrice, draw, false);
                }

                draw.refresh();

                for (j = 0; j < pieces.size(); j++) {
                    pieces.get(j).PieceDown(lines[i]);
                }

                for (j = 0; j < pieces.size(); j++) {
                    pieces.get(j).Draw(matrice, draw, true);
                }

                draw.refresh();
            }
        }

        /**
         * debug
         */
    	//~ if (line_count>0)
        //~ {
        //~ System.out.println("THEY HAVE BEEN DELETED :");
        //~ IA.display_matrice(matrice.matrice);
        //~ }	
        
        // nettoyage des pièces
        if (line_count > 0)
        {
			for (j = 0; j < pieces.size(); j++)
			{
				if (pieces.get(j).Cleared() == true)
				{
					//~ System.out.println("Piece "+j+" supprimee");
					pieces.remove(j);
					j--;
				}
			}
		}
        
        // on renvoie les points gagnés
        switch (line_count) {
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

    public static void CheckStuck() {
        if (current_piece.p.IsStuck() == true) {
            // on ajoute la pièce courante dans le tableau et on génère la nouvelle
            RandNewPiece();

            // les pièces ne descendent que d'une case max par ligne complétée
            // une complétion de lignes en cascade est donc impossible
            points += CheckLine();

            current_piece.p.Draw(matrice, draw, true);
            draw.refresh();
        }
    }

    public static boolean CheckCollision() {
        for (int i = 0; i < pieces.size(); i++) {
            if (current_piece.p.IsCollidedWith(pieces.get(i)) == true) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // Connexion IA
        try {
            current_piece = new CurrentPiece();
            Naming.rebind("piece", current_piece);
        } catch (RemoteException re) {
            System.out.println(re);
        } catch (MalformedURLException e) {
            System.out.println(e);
        }

        try {
            matrice = new Matrice();
            Naming.rebind("matrice", matrice);
        } catch (RemoteException re) {
            System.out.println(re);
        } catch (MalformedURLException e) {
            System.out.println(e);
        }

        // Base Graphique
        final JFrame f = new JFrame("TetriS");
        draw = new Draw(matrice);    //fenêtre de jeu Tetris
        JPanel separ = new JPanel(); // séparateur
        JPanel panel = new JPanel(); //fenêtre de droite Score/Nb Lignes
        
        JSeparator sep = new JSeparator();
        JLabel lblTitreScore = new JLabel("Score :");
        final JLabel lblScore = new JLabel("0");
        JLabel lblTitreNbLignes = new JLabel("Nombre de lignes :");
        final JLabel lblNbLignes = new JLabel("0");
        //layout
        GridBagConstraints gridBagConstraints;
        panel.setLayout(new GridBagLayout());
        
        sep.setOrientation(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1,310));
        separ.add(sep);
        
        lblTitreScore.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitreScore.setHorizontalTextPosition(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
 
        gridBagConstraints.ipady = 10;
        panel.add(lblTitreScore, gridBagConstraints);

        lblScore.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipady = 10;
        panel.add(lblScore, gridBagConstraints);

        lblTitreNbLignes.setHorizontalAlignment(SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 30;
        gridBagConstraints.ipady = 10;
        panel.add(lblTitreNbLignes, gridBagConstraints);
        
        lblNbLignes.setHorizontalAlignment(SwingConstants.CENTER);
        lblNbLignes.setHorizontalTextPosition(SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipady = 10;
        panel.add(lblNbLignes, gridBagConstraints);
        //fin layout
        f.getContentPane().add(draw, BorderLayout.WEST);
        f.getContentPane().add(separ, BorderLayout.CENTER);
        f.getContentPane().add(panel, BorderLayout.EAST);
        f.setResizable(false); //empêche le redimensionnement de la fenêtre
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); //VAL : DISPOSE -> EXIT,     
        f.pack();                                                  //sinon le prog tournait derrière
        //après qu'on ait cliqué sur la croix
        f.setLocationRelativeTo(null); //centrage de la fenêtre de jeu (val)
        f.setVisible(true);

        // Clavier
        f.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_RIGHT:
                        action("right");
                        break;
                    case KeyEvent.VK_LEFT:
                        action("left");
                        break;
                    case KeyEvent.VK_UP:
                        action("rotate");
                        break;
                    case KeyEvent.VK_DOWN:
                        action("down");
                        break;
                    case KeyEvent.VK_SPACE:
                        action("fall");
                        break;
                }
                CheckStuck();
            }

            public void keyReleased(KeyEvent e) {
                // KEY RELEASED
            }
        });

        RandNewPiece();
        pieces = new ArrayList<Piece>();

        // init timer
        Timer timer_fall = new Timer();
        Timer timer_stuck = new Timer();

        timer_fall.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        action("down");
                        CheckStuck();
                        lblScore.setText(String.valueOf(points));
                        lblNbLignes.setText(String.valueOf(total_lines));
                    }
                },
                1 * 1000,
                1 * 1000);
    }
}
