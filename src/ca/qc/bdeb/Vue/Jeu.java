/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.qc.bdeb.Vue;

import ca.qc.bdeb.Controleur.Controleur;
import static ca.qc.bdeb.Controleur.Controleur.HAUTEUR;
import static ca.qc.bdeb.Controleur.Controleur.HAUTEUR_SOL;
import static ca.qc.bdeb.Controleur.Controleur.LARGEUR;
import ca.qc.bdeb.Modele.Modele;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static java.lang.Math.random;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.input.KeyCode;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

/**
 *
 * @author emuli
 */
public class Jeu extends BasicGame implements Observer {

    private Controleur controleur;
    private Modele modele;
    private CopyOnWriteArrayList<Bougeable> listeBougeable = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Entite> listeEntite = new CopyOnWriteArrayList<>();
    private ArrayList<KeyCode> listeKeys = new ArrayList<>(); // Les touches enfoncées
    private Input input; // L’entrée (souris/touches de clavier, etc.)
    private SpriteSheet spriteSheetTiles, spriteSheetDivers,
            spriteSheetCoeur, spriteSheetMonde, spriteSheetPrincesse,
            spriteSheetBonus;

    public static final float buffer = 50;
    private Princess princesse;
    private boolean isFinDePartie = false;
    private int vies, points, decallage, intervaleHauteur, indexBackGround;
    private Random random = new Random();

    /**
     * Contructeur de Jeu
     *
     * @param gamename Le nom du jeu
     * @param controleur Le controleur du jeu
     * @param modele Le modèle du jeu
     */
    public Jeu(String gamename, Controleur controleur, Modele modele) {
        super(gamename);
        this.modele = modele;
        this.controleur = controleur;
        modele.addObserver(this);
        // …
    }

    @Override
    public void init(GameContainer container) throws SlickException {
        input = container.getInput();

        spriteSheetBonus = new SpriteSheet("images/sprites_divers.png", 32, 32);
        spriteSheetDivers = new SpriteSheet("images/sprites_divers.png", 32, 32);
        spriteSheetMonde = new SpriteSheet("images/sprites_monde.png", 32, 32);
        spriteSheetTiles = new SpriteSheet("/images/tiles.png", 32, 32);
        spriteSheetPrincesse = new SpriteSheet("images/sprites_princess.png", 32, 64);
        spriteSheetCoeur = new SpriteSheet("/images/coeur.png", 32, 32);

        vies = modele.getHealthPoints();
        waveTimer.start();
        decallage = HAUTEUR - HAUTEUR_SOL;
        indexBackGround = listeEntite.size();

        initCiel();
        initPlancher();
        initPrincesse();

        Bonus bonus = new BonusVie(136, 136, spriteSheetDivers);
        listeEntite.add(bonus);
        listeBougeable.add(bonus);

        intervaleHauteur = (int) (HAUTEUR - HAUTEUR_SOL - princesse.getHeight());

    }

    @Override
    public void update(GameContainer gc, int i) throws SlickException {
        for (Bougeable bougeable : listeBougeable) {
            bougeable.bouger();
        }
        deplacerBackground();
        enleverEntitesExterieurEcran();

        if (!isFinDePartie) {

            getKeys();
            traiterKeys();

            gererCollisionJoueurEnnemi();
            gererCollisionBulletEnnemi();
            gererCollisionJoueurBonus();
        }

        if (isFinDePartie) {
            if (input.isKeyDown(Input.KEY_R)) {
                restartGame();
            }
        }

    }

    @Override
    public void render(GameContainer gc, Graphics g) throws SlickException {
        for (Entite entite : listeEntite) {
            g.drawImage(entite.getImage(), entite.getX(), entite.getY());
        }

        g.drawString("Points: " + points, LARGEUR - buffer * 5, 10);

        int posXCoeur = 16, posYCoeur = 32;
        for (int i = 0; i < vies; i++) {
            g.drawImage(spriteSheetCoeur, posXCoeur, posYCoeur);
            posXCoeur += 32;
        }

        if (isFinDePartie) {
            princesse.falling();
            //stopBackgroundFromMoving();
            g.drawString("GAME OVER. PRESS R TO RESTART", (LARGEUR / 2) - buffer, HAUTEUR / 2);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        isFinDePartie = modele.isFinDePartie();
        if (isFinDePartie) {
            // tempsCompteARebours = System.currentTimeMillis() + 2500;
            //clearCollisionableList();
            stopBackgroundFromMoving();
            //beach doit crash
            //game over pour 2 sec+ freeze app
            // restartGame();

        }
        vies = modele.getHealthPoints();
        points = modele.getPoints();
    }

    private void clearCollisionableList() {
        ArrayList<Entite> listeTemp = new ArrayList();
        for (Entite entite : listeEntite) {
            if (entite instanceof Collisionable) {
                listeTemp.add(entite);
            }
        }
        listeEntite.removeAll(listeTemp);
        listeBougeable.removeAll(listeTemp);
    }

    private void getKeys() {
        if (input.isKeyDown(Input.KEY_SPACE)) {
            if (!listeKeys.contains(KeyCode.SPACE)) {
                listeKeys.add(KeyCode.SPACE);
            }
        } else {
            listeKeys.remove(KeyCode.SPACE);
        }

        //FLECHE DROITE
        if (input.isKeyDown(Input.KEY_RIGHT) || input.isKeyDown(Input.KEY_D)) {
            if (!listeKeys.contains(KeyCode.RIGHT)) {
                listeKeys.add(KeyCode.RIGHT);
            }
        } else {
            listeKeys.remove(KeyCode.RIGHT);

        }
        if (input.isKeyDown(Input.KEY_LEFT) || input.isKeyDown(Input.KEY_A)) {
            if (!listeKeys.contains(KeyCode.LEFT)) {
                listeKeys.add(KeyCode.LEFT);
            }
        } else {
            listeKeys.remove(KeyCode.LEFT);

        }
        if (input.isKeyDown(Input.KEY_UP) || input.isKeyDown(Input.KEY_W)) {
            if (!listeKeys.contains(KeyCode.UP)) {
                listeKeys.add(KeyCode.UP);
            }
        } else {
            listeKeys.remove(KeyCode.UP);

        }
        if (input.isKeyDown(Input.KEY_DOWN) || input.isKeyDown(Input.KEY_S)) {
            if (!listeKeys.contains(KeyCode.DOWN)) {
                listeKeys.add(KeyCode.DOWN);
            }
        } else {
            listeKeys.remove(KeyCode.DOWN);

        }
    }

    private void traiterKeys() throws SlickException {
        princesse.bouger(listeKeys);
        if (listeKeys.contains(KeyCode.SPACE) && princesse.peutAttaquer()) {
            tirerBalle();
        }

    }
    private javax.swing.Timer waveTimer = new javax.swing.Timer(5000, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                //  if (!isFinDePartie) {
                declencherWaveEnnemis();
                // }
            } catch (Exception ex) {
                Logger.getLogger(Jeu.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    });

    public boolean isBonusTriple() {
//        if (tempsBonusTriple + 10000 < System.currentTimeMillis()) {
//            return false;
//        }
        return true;
    }

    private void tirerBalle() throws SlickException {
        if (!isBonusTriple()) {
            Bullet bullet = new Bullet(princesse.getX() + princesse.getWidth(), princesse.getY() + (princesse.getHeight() / 4));
            listeBougeable.add(bullet);
            listeEntite.add(bullet);
        } else {
            Bullet bullet = new Bullet(princesse.getX() + princesse.getWidth(), princesse.getY() + (princesse.getHeight() / 4));
            listeBougeable.add(bullet);
            listeEntite.add(bullet);
            Bullet bullet1 = new Bullet(princesse.getX() + princesse.getWidth(), princesse.getY() + (princesse.getHeight() / 4));
            bullet.setDeltaY(12);
            listeBougeable.add(bullet1);
            listeEntite.add(bullet1);
            Bullet bullet2 = new Bullet(princesse.getX() + princesse.getWidth(), princesse.getY() + (princesse.getHeight() / 4));
            bullet2.setDeltaY(-12);
            listeBougeable.add(bullet2);
            listeEntite.add(bullet2);
        }
    }

    private void deplacerBackground() {
        for (Entite entite : listeEntite) {
            if (entite instanceof Plancher) {
                if (entite.getX() + entite.getWidth() <= 0) {
                    entite.setX(LARGEUR);

                }
            }
        }

    }

    private void initPrincesse() {
        princesse = new Princess(64, 64, spriteSheetPrincesse);
        listeEntite.add(princesse);

    }

    private void initPlancher() {
        int hauteurPlancher = 2;
        for (int i = 0; i <= LARGEUR + 32; i += 32) {
            for (int j = 0; j <= hauteurPlancher; j++) {
                Plancher plancher = new Plancher(i, HAUTEUR - j * 32, spriteSheetTiles);
                listeBougeable.add(plancher);
                listeEntite.add(plancher);
            }
        }
    }

    private void initCiel() {
        for (int i = 0; i < LARGEUR; i += 32) {
            for (int j = 0; j < HAUTEUR; j += 32) {
                Ciel ciel = new Ciel(i, j, spriteSheetTiles);
                listeBougeable.add(ciel);
                listeEntite.add(ciel);

            }
        }
    }

    private void gererCollisionBulletEnnemi() throws SlickException {
        ArrayList<Entite> listeTemp = new ArrayList();
        for (Bougeable b1 : listeBougeable) {

            for (Bougeable b2 : listeBougeable) {
                if ((b1 instanceof Ennemi && b2 instanceof Bullet)) {

                    if (b1.getRectangle().intersects(b2.getRectangle())) {

                        listeTemp.add((Entite) b1);
                        listeTemp.add((Entite) b2);

                        controleur.elimineEnnemis();
                    }
                }
            }
        }

        if (!listeTemp.isEmpty()) {
            int r = random.nextInt(10);
            if (r == 1) {
                spondBonus(256, 256);

                //spondBonus(listeTemp.get(0).getRectangle().getX(), listeTemp.get(0).getRectangle().getY());
                System.out.println("Bonus empty : " + listeTemp.isEmpty());
            }
        }
        listeEntite.removeAll(listeTemp);
        listeBougeable.removeAll(listeTemp);
    }

    private void spondBonus(float x, float y) throws SlickException {
        int choixBonus = random.nextInt(3);
        //  int choixBonus = 0;

        Bonus bonus = null;
        switch (choixBonus) {
            case 0:

                System.out.println("x:" + x);
                System.out.println("y:" + y);
                bonus = new BonusVie(x, y, spriteSheetDivers);
                break;
            case 1:
//                bonus = new BombeMega(x, y, spriteSheetBonus);

                break;
            case 2:
//                bonus = new ArmeABalles(x, y, spriteSheetBonus);
                break;
            default:
        }

        listeEntite.add(indexBackGround, bonus);
        listeBougeable.add((Bougeable) bonus);
    }

    private void gererCollisionJoueurEnnemi() {
        ArrayList<Entite> listeTemp = new ArrayList();

        for (Bougeable bougeable : listeBougeable) {
            if (bougeable instanceof Ennemi || bougeable instanceof BulletEnnemi) {
                if (princesse.getRectangle().intersects(bougeable.getRectangle())) {
                    listeTemp.add((Entite) bougeable);
                    controleur.perdVie();
                }
            }
        }

        listeEntite.removeAll(listeTemp);
        listeBougeable.removeAll(listeTemp);

    }

    private void gererCollisionJoueurBonus() {
        ArrayList<Entite> listeTemp = new ArrayList();
        Bonus bonus = null;

        for (Bougeable bougeable : listeBougeable) {
            if (bougeable instanceof Bonus) {
                if (princesse.getRectangle().intersects(bougeable.getRectangle())) {
                    listeTemp.add((Entite) bougeable);

                    controleur.ramasseBonus();
                    bonus = (Bonus) bougeable;

                }
            }
        }

        if (!listeTemp.isEmpty()) {
            activerBonus(bonus);
        }

        listeBougeable.removeAll(listeTemp);
        listeEntite.removeAll(listeTemp);

    }

    private void enleverEntitesExterieurEcran() {
        ArrayList<Entite> listeTemp = new ArrayList();

        for (Entite entite : listeEntite) {
            if (entite.getDetruire()) {
                listeTemp.add(entite);
            }
        }
        listeEntite.removeAll(listeTemp);
        listeBougeable.remove(listeTemp);
    }

    private void activerBonus(Bonus bonus) {

        if (bonus instanceof BombeMega) {
            activerBonusBombe(bonus);
        }
        if (bonus instanceof BonusVie) {
            controleur.ramasseBonusVie();
        }
//        if (bonus instanceof ArmeABalles) {
//            //tempsBonusTriple = System.currentTimeMillis();
//
//        }

    }

    private void declencherWaveEnnemis() throws SlickException {
        int choixTypeEnnemi = random.nextInt(2);
//        int choixTypeEnnemi = 1;

        switch (choixTypeEnnemi) {
            case 0:
                creerEnnemisBulles();
                break;
            case 1:
                creerEnnemiRocket();
            default:

        }
    }

    private int getNumberEnnemisSpawned() {
        int nbEnnemis;
        do {
            nbEnnemis = random.nextInt(6) * 2;//pour que ca soit pair
        } while (nbEnnemis == 0);

        return nbEnnemis;
    }

    private void creerEnnemisBulles() {
        for (int i = 0; i < getNumberEnnemisSpawned(); i++) {
            EnnemiBulle ennemiBulle = new EnnemiBulle(LARGEUR,
                    decallage - buffer + i * 16, spriteSheetDivers);
            listeEntite.add(ennemiBulle);
            listeBougeable.add(ennemiBulle);
        }
    }

    private void creerEnnemiRocket() {
        int hauteur = random.nextInt(intervaleHauteur);
        EnnemiRocket ennemiRocket = new EnnemiRocket(LARGEUR, hauteur, spriteSheetDivers);
        listeEntite.add(ennemiRocket);
        listeBougeable.add(ennemiRocket);
    }

    private void restartGame() {
        modele.restartPartie();
//        makeBackgroundMoveAgain();
        clearCollisionableList();
        princesse.setLocation(64, HAUTEUR - HAUTEUR_SOL - princesse.getHeight());

    }

    private void makeBackgroundMoveAgain() {

    }

    private void stopBackgroundFromMoving() {

        //todo
        ArrayList<Entite> listeTemp = new ArrayList();

        for (Bougeable bougeable : listeBougeable) {
            if (bougeable instanceof Background) {
                listeTemp.add((Entite) bougeable);
            }
        }

    }

    private void activerBonusBombe(Bonus bonus) {
        ArrayList<Entite> listeTemp = new ArrayList();

        for (Bougeable bougeable : listeBougeable) {
            if (bougeable instanceof Ennemi && ((Entite) bougeable).getX() < LARGEUR) {
                listeTemp.add((Entite) bougeable);
            }
        }

        int nbEnnemisTues = listeTemp.size();
        controleur.ramasseBombeMega(nbEnnemisTues);

        listeEntite.removeAll(listeTemp);
        listeBougeable.removeAll(listeTemp);
    }

}
