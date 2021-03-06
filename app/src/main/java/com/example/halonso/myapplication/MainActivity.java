package com.example.halonso.myapplication;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;

import static java.lang.Math.sqrt;


//le code pour choper les photos de la gallery viens à 100% de http://stackoverflow.com/questions/10473823/android-get-image-from-gallery-into-imageview


public class MainActivity extends AppCompatActivity {// pour utiliser un dico





    class Selecteur {
        public int cptactu;
        private int nbimages;
        private ArrayList<Imagetraitement> images;

        public Selecteur() {
            cptactu = 0;
            nbimages = 0;
            images = new ArrayList<Imagetraitement>();
        }

        public void add(Bitmap b, int density) {
            b.setDensity(density);
            Imagetraitement c = new Imagetraitement(b);
            int pos = cptactu + 1;
            if (cptactu == nbimages) {
                pos = 0;
            }
            images.add(pos, c);
            nbimages++;
            next();
        }

        public void remove(int a) {
            images.remove(a);
            nbimages--;
        }

        public Imagetraitement getimagetraiement() {
            return images.get(cptactu);
        }

        public void next() {
            if (cptactu < nbimages - 1) {
                cptactu++;
            } else {
                cptactu = 0;
            }
        }

        public void previous() {
            if (cptactu != 0) {
                cptactu--;
            } else {
                cptactu = nbimages - 1;
            }
        }

    }

    class Pos{
        int x;
        int y;
        public Pos(int a,int b){
            x=a;
            y=b;
        }
    }



    class Actu extends Imagetraitement{
        int posx;//la position de l'image actuelle dans l'image view (coin sup gauche)
        int posy;//

        int xapreszoom;// si y a eu un zoom, on garde l'info.
        int yapreszoom;

        LinkedList<Imagetraitement> liste;

        public Actu(Imagetraitement i){
            super(i);
            rotation=0;
            posy=0;
            posx=0;
            xapreszoom=width;
            yapreszoom=height;
            liste=new LinkedList<>();
            liste.addLast(i);

        }

        public Actu(Bitmap bitmap) {
            super(bitmap);
            rotation=0;
            posy=0;
            posx=0;
            xapreszoom=width;
            yapreszoom=height;
            liste=new LinkedList<>();
            Imagetraitement im=new Imagetraitement(bitmap);
            liste.addLast(im);
        }

        public Bitmap afficheuse(){//à Fiiiiiiiiinir( pour gérer la rotation, le zoom...
            Bitmap affiche = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            affiche.setDensity(resolution);// à faire : remplacer tout les getdensity par density ecran
            affiche.setPixels(tableauimage, 0, width, 0, 0, width, height);
            return  affiche;

        }

        public void affect(Bitmap b){
            width=b.getWidth();
            height=b.getHeight();
            tableauimage=new int[width*height];
            b.getPixels(tableauimage, 0, width, 0, 0, width, height);

        }

        public void affect(Imagetraitement im){
            width=im.width;
            height=im.height;
            tableauimage=im.tableauimage;
        }

        public Pos getpixel(int x, int y){//à Fiiiiiiiiinir( pour gérer la rotation, le zoom...
            return new Pos(x,y);
        }

    }
    class Actu2 {// a voir
        int posx;//la position de l'image dans l'image view (coin sup gauche)
        int posy;//
        LinkedList<Imagetraitement> liste;

        public Actu2() {
            posy = 0;
            posx = 0;
            int xapreszoom;// si y a eu un zoom, on garde l'info.
            int yapreszoom;
            liste = new LinkedList<>();


        }



        public Bitmap afficheuse() {//à Fiiiiiiiiinir( pour gérer la rotation, le zoom...
            Imagetraitement i=liste.getLast();
            Bitmap affiche = Bitmap.createBitmap(i.width, i.height, Bitmap.Config.ARGB_8888);
            affiche.setDensity(resolution);// à faire : remplacer tout les getdensity par density ecran
            affiche.setPixels(i.tableauimage, 0, i.width, 0, 0, i.width, i.height);
            return affiche;

        }

        public void add(Bitmap b) {
            Imagetraitement i=new Imagetraitement(b);
            liste.add(i);

        }

        public void add(Imagetraitement im) {
            liste.add(im);
        }

        public Pos getpixel(int x, int y) {//à Fiiiiiiiiinir( pour gérer la rotation, le zoom...
            return new Pos(x, y);
        }
    }

    float[][] tabledesdistance;//j'aimerais bien stocké toute les distances pour aller vraiment plus vite (nottament pour augmenter la vitesse des algos de contour et pastel)
    ImageView image;
    int imageheight;//taille de la view
    int imagewidth;//taille de la view;
    int resolution;
    TextView txt;
    TextView coordonne;
    TextView nouveaupix;
    Selecteur selecteur;
    SeekBar seekbar1 = null;
    Actu btmpactu; // ce bitmap permet de stocké l'image avant un résultat.



    Set<Integer> couleurs;// de temps en temps pour des fonctions compliqué, j'ai besoin de set ou d'hashtable.
    Hashtable<Integer, Imagetraitement.Customclass> table;
    private static int RESULT_LOAD_IMAGE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        resolution = metrics.densityDpi;
        final int widthphone = metrics.widthPixels;
        final int heightphone = metrics.heightPixels;
        Log.i("DISPLAY", "densityDpi : " + resolution + "\n" + "width : " + widthphone + "\n" + "height : " + heightphone);

        final BitmapFactory.Options mutableandnoscalabe = new BitmapFactory.Options();
        mutableandnoscalabe.inMutable = true;
        mutableandnoscalabe.inScaled = false;
        /*tabledesdistance = new float[0xFFFFFF][0xFFFFFF];
        for (int i=0;i< 0xFFFFFF;i++){//preparation de la table des distances entrecouleurs
            for (int j=0; j<i;j++){
                int redi = (i >> 16) & 0xFF;
                int greeni = (i >> 8) & 0xFF;
                int bluei = i & 0xFF;
                int redj = (i >> 16) & 0xFF;
                int greenj = (i >> 8) & 0xFF;
                int bluej = i & 0xFF;
                float d=(float)sqrt((redi-redj)*(redi-redj)+(greeni-greenj)*(greeni-greenj)+(bluei-bluej)*(bluei-bluej));
                tabledesdistance[i][j]=d;
                tabledesdistance[j][i]=d;
            }*/


        selecteur = new Selecteur();

        image = (ImageView) findViewById(R.id.image);

        imageheight = image.getHeight();
        imagewidth = image.getWidth();//marche pas : met a 0
        coordonne = (TextView) findViewById(R.id.coordonne);
        nouveaupix= (TextView) findViewById(R.id.nouveaupixel);

        image.setOnTouchListener(new View.OnTouchListener() {
            int debutx, debuty;
            int posx;
            int posy;
            int cptfauxmoovex;
            int cptfauxmoovey;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Bitmap actu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                int heightbitmap = actu.getHeight();
                int widthbitmap = actu.getWidth();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int xt = (int) (event.getX() + 0.5);
                    int yt = (int) (event.getY() + 0.5);
                    debutx = xt;
                    posx = xt;
                    debuty = yt;
                    posy = yt;
                    cptfauxmoovex = 0;
                    cptfauxmoovey = 0;
                    //la, c'est les coordonnées dan l'image view.

                    int x = xt - btmpactu.posx;
                    int y = yt - btmpactu.posy;
                    if (x < widthbitmap && x >= 0 && y >= 0 && y < heightbitmap) {

                        int imview = actu.getPixel(x, y);

                        int newalpha=imview >>> 24;
                        int newr = (imview >> 16) & 0xFF;
                        int newg = (imview >> 8) & 0xFF;
                        int newb = imview & 0xFF;
                        nouveaupix.setText("Apres: (" + newalpha+", " + newr + ", " + newg + ", " + newb + ")");
                    }
                    if (x<btmpactu.width  && x >= 0 && y >= 0 && y < btmpactu.height ) {
                        Pos poscorrespondante = btmpactu.getpixel(x, y);
                        int colorbtmactu= btmpactu.tableauimage[poscorrespondante.x+ poscorrespondante.y*btmpactu.width];
                        int alphancien = colorbtmactu >>> 24;
                        int rancien = (colorbtmactu >> 16) & 0xFF;
                        int gancien = (colorbtmactu >> 8) & 0xFF;
                        int bancien = colorbtmactu & 0xFF;
                        coordonne.setText("(" + x + "," + y + ")\n Avant: (" + alphancien + ", " + rancien + ", " + gancien + ", " + bancien + ")");
                    }
                    else{
                        coordonne.setText("n'éxistait pas!");
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    int x = (int) (event.getX() + 0.5);
                    int y = (int) (event.getY() + 0.5);
                    int dx = x - posx;
                    int dy = y - posy;
                    int decalagex = posx - debutx + btmpactu.posx - cptfauxmoovex;//vrai position de l'image dans l'écran
                    int decalagey = posy - debuty + btmpactu.posy - cptfauxmoovey;

                    int margex = 300;//ici on regle la marge
                    int margey = 300;
                    // à fair pur clarifier; aussi, non seulement dx=0 mais aussi ramener pos sur le mur (sinon on peut depasser le mur)
                    int murgauche = -widthbitmap + margex;
                    int murdroit = imagewidth - margex;
                    coordonne.setText(decalagex + " " + decalagey);
                    int murhaut = -heightbitmap + margey;
                    int murbas = imageheight - margey;
                    posx += dx;
                    posy += dy;
                    if (-dx < 0) {
                        if (decalagex - dx > murdroit) {
                            cptfauxmoovex += dx;
                            dx = 0;
                        }
                    } else {
                        if (decalagex - dx < murgauche) {
                            cptfauxmoovex += dx;
                            dx = 0;
                        }
                    }
                    if (-dy < 0) {
                        if (decalagey - dy > murbas) {
                            cptfauxmoovey += dy;
                            dy = 0;
                        }
                    } else {
                        if (decalagey - dy < murhaut) {
                            cptfauxmoovey += dy;
                            dy = 0;
                        }
                    }
                    image.scrollBy(-dx, -dy);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    btmpactu.posx += posx - debutx - cptfauxmoovex;
                    btmpactu.posy+= posy - debuty - cptfauxmoovey;

                }
                return true;

            }
        }


        );
        txt = (TextView) findViewById(R.id.txtHello);
        txt.setText(" " + widthphone + " " + heightphone);
        seekbar1 = (SeekBar) findViewById(R.id.bar);
        seekbar1.setMax(1000);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.zoomtest, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.contraste3, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.fantome, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.testcontraste, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.chats, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.tapir, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.testcontrastedeux, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.testcontrastequatre, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.lenna, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.fantomecinq, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.fantomedeux, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.meuf, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.lenna512, mutableandnoscalabe), resolution);
        selecteur.add(BitmapFactory.decodeResource(getResources(), R.drawable.melench, mutableandnoscalabe), resolution);


        Bitmap blanc = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
        int[] tabbitmap = new int[200 * 200];
        for (int i = 0; i < 200 * 200; i++) {
            tabbitmap[i] = 0xFFFF0001;

        }
        blanc.setPixels(tabbitmap, 0, 200, 0, 0, 200, 200);

        selecteur.add(blanc, resolution);
        btmpactu=new Actu(selecteur.getimagetraiement());
        image.setImageBitmap(btmpactu.afficheuse());

        final Button addcolor = (Button) findViewById(R.id.addcolor);


        final Button next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addcolor.setVisibility(View.INVISIBLE);
                long t0 = System.currentTimeMillis();
                seekbar1.setVisibility(View.INVISIBLE);
                //selecteur.getimagetraiement().imagebase=((BitmapDrawable) image.getDrawable()).getBitmap();//on sauvegarde les modifs
                selecteur.next();
                Imagetraitement imageselected = selecteur.getimagetraiement();
                btmpactu = new Actu(imageselected);
                image.setImageBitmap(btmpactu.afficheuse());
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + image.getHeight() + " " + image.getWidth());
                image.scrollTo(0, 0);
                btmpactu.posx = 0;
                btmpactu.posy = 0;
            }
        });

        final Button previous = (Button) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addcolor.setVisibility(View.INVISIBLE);
                long t0 = System.currentTimeMillis();
                seekbar1.setVisibility(View.INVISIBLE);
                //selecteur.getimagetraiement().imagebase=((BitmapDrawable) image.getDrawable()).getBitmap();
                selecteur.previous();
                Imagetraitement imageselected = selecteur.getimagetraiement();
                btmpactu = new Actu(imageselected);
                image.setImageBitmap(btmpactu.afficheuse());
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);
                image.scrollTo(0, 0);
                btmpactu.posx = 0;
                btmpactu.posy = 0;
            }
        });


        Button buttonLoadImage = (Button) findViewById(R.id.buttonLoadPicture);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
                image.scrollTo(0, 0);
                btmpactu.posx = 0;
                btmpactu.posy = 0;
            }
        });
    }

    //on gere le menu ici
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        final Button addcolor = (Button) findViewById(R.id.addcolor);// Pk je dois le rajouter la? me faut un cour sur la portée des variables

        switch (item.getItemId()) {// à faire demander à la prof pk parfois l0 doit etre redéfinie et d'autre non.
            case R.id.action_save:
                selecteur.add(((BitmapDrawable) image.getDrawable()).getBitmap(), resolution);

                return true;

            case R.id.undo:
                if (!btmpactu.liste.isEmpty()) {
                    btmpactu.liste.removeLast();
                    btmpactu.affect(btmpactu.liste.peekLast());
                    image.setImageBitmap(btmpactu.afficheuse());
                }

                return true;

            case R.id.action_delete:
                addcolor.setVisibility(View.INVISIBLE);
                selecteur.remove(selecteur.cptactu);
                selecteur.next();
                btmpactu= new Actu(selecteur.getimagetraiement());// y a pas moy de pas faire de new?
                image.setImageBitmap(btmpactu.afficheuse());
                return true;

            case R.id.adjust:
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap());

                int wview = imagewidth;
                int hview = imageheight;
                double ratiow = (double) wview / btmpactu.width;
                double ratioh = (double) hview / btmpactu.height;
                int newwidth=wview;
                int newheight=hview;
                if (ratiow < ratioh) {
                    newheight=(int) (ratiow * btmpactu.height + 0.5);

                } else {
                    newwidth=(int) (ratioh * btmpactu.width + 0.5);
                }
                btmpactu.xapreszoom=newwidth;
                btmpactu.yapreszoom=newheight;
                Imagetraitement im19=btmpactu.redimensionner(newwidth,newheight);
                image.setImageBitmap(im19.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                image.scrollTo(0, 0);
                btmpactu.posx = 0;
                btmpactu.posy = 0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + imageheight + " " + imagewidth);

                return true;



            case R.id.redimensionner://mettre à jour postioion dans imview
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap()); // srx ce new me soul
                seekbar1.setProgress(seekbar1.getMax() / 5);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = ((double) i / seekBar.getMax()) * 5;
                            if (valeurseek != 0) {
                                int w = btmpactu.width;
                                int h = btmpactu.height;
                                int newWidth = (int) ((w * valeurseek) + 0.5);
                                int newHeight = (int) ((h * valeurseek) + 0.5);
                                int decalw = (newWidth - w) / 2;
                                int decalh = (newHeight - h) / 2;
                                image.scrollTo(decalw - btmpactu.posx, decalh - btmpactu.posy);
                                long t0 = System.currentTimeMillis();
                                //image.setImageBitmap(redimensionner(btmpactu, (int) ((w * valeurseek) + 0.5), (int) ((h * valeurseek) + 0.5)));
                                Imagetraitement im=btmpactu.redimensionner((int) ((w * valeurseek) + 0.5), (int) ((h * valeurseek) + 0.5));
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;

                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;


            case R.id.pixelisation:
                addcolor.setVisibility(View.INVISIBLE);
               Bitmap btm= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm));
                btmpactu.affect(btm);
                seekbar1.setProgress(1);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int)( (double)(i*50)/seekbar1.getMax()+1);
                            long t0 = System.currentTimeMillis();
                            Imagetraitement im=btmpactu.pixelisation(valeurseek);
                            image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;
            case R.id.griser://200,150,70,0.17)
                addcolor.setVisibility(View.INVISIBLE);
                Bitmap btm2= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement());
                btmpactu.affect(btm2); // srx ce new me soul
                long t0 = System.currentTimeMillis();
                //image.setImageBitmap(toGray2(btmpactu));
                Imagetraitement im=btmpactu.toGray2();
                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);

                return true;

            case R.id.grisersaufteinte://200,150,70,0.17)
                addcolor.setVisibility(View.INVISIBLE);
                Bitmap btm3= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm3));
                btmpactu.affect(btm3); // srx ce new me soul
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                            //image.setImageBitmap(grisersaufteinte2(btmpactu, 200, 150, 70, 0.17, valeurseek));
                            Imagetraitement im=btmpactu.grisersaufteinte2(200,150,70,0.17,valeurseek);
                            image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));

                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.fusion:
                addcolor.setVisibility(View.INVISIBLE);
                Bitmap btm4= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm4));
                btmpactu.affect(btm4);
                t0 = System.currentTimeMillis();
                //Bitmap fufu = fusionneri1dansi2(((BitmapDrawable) image.getDrawable()).getBitmap(), ((BitmapDrawable) image.getDrawable()).getBitmap(), 300, 0);
                Imagetraitement im2=btmpactu.fusionneri1dansi2(btmpactu,300,0);
                image.setImageBitmap(im2.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                //image.setImageBitmap(fufu);
                t1 = System.currentTimeMillis();
                t2 = t1 - t0;
                txt.setText(im2.height + "  " + im2.width + " " + t2 + " " + image.getHeight() + " " + image.getWidth());

                return true;

            case R.id.vieux://200,150,70,0.17)
                addcolor.setVisibility(View.INVISIBLE);
                Bitmap btm5= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm5));
                btmpactu.affect(btm5); // srx ce new me soul
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                            //image.setImageBitmap(vieeux(btmpactu, valeurseek));
                            Imagetraitement im2=btmpactu.vieeux(valeurseek);
                            image.setImageBitmap(im2.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.pixelart://200,150,70,0.17)
                Bitmap btm6= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm6));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm6); // srx ce new me soul
                seekbar1.setProgress(1);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) (((double) i * 50 / seekBar.getMax()) + 0.5);
                            if (valeurseek == 0) {
                                image.setImageBitmap(btmpactu.afficheuse());
                            } else {
                                long t0 = System.currentTimeMillis();
                                //image.setImageBitmap(vieeux(btmpactu, valeurseek));
                                Imagetraitement im2 = btmpactu.pixelisation(valeurseek).intelligentpastelisationtable2(10);
                                image.setImageBitmap(im2.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.lum://200,150,70,0.17)
                Bitmap btm7= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm7));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm7); // srx ce new me soul
                seekbar1.setProgress(seekbar1.getMax() / 10);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = ((double) i / seekBar.getMax() * 10);
                            long t0 = System.currentTimeMillis();
                            Imagetraitement im=btmpactu.lum(valeurseek);
                            image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));// bon ça peut paraitre compliqué de passer par une fonction afficheuse, mais ça permet de prendre
                            //le point de vue: quand l'utilisateur zoom, il ne veut pas redimensionner l'image, il veut "changer de vue" sur l'image.
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.flou://200,150,70,0.17)
                final Bitmap btm8= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm8));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm8); // srx ce new me soul
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int min;
                            if (btmpactu.width > btmpactu.height) {
                                min = (btmpactu.height >> 1);
                            } else {
                                min = (btmpactu.width >> 1);
                            }

                            int valeurseek = (int) (((double) i / seekBar.getMax() * min) + 0.5);
                            if (valeurseek < min && valeurseek!=0) {
                                Log.i("valseek", " " + valeurseek);
                                long t0 = System.currentTimeMillis();
                                //Imagetraitement im = btmpactu.fou(valeurseek);
                                //image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0, resolution));
                                image.setImageBitmap(flourapide(btm8,valeurseek));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                            else{
                                if (valeurseek!=0) {
                                    txt.setText("Houla, t'emballe pas");
                                }
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.teinterapide:
                Bitmap btm9= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm9));
                addcolor.setVisibility(View.INVISIBLE);
                t0 = System.currentTimeMillis();// ici c'est trop bizarre, mes ti et im bug si je les apelle t0 t1 t2 et im
                //image.setImageBitmap(teintrapide(0, 255, 120, ((BitmapDrawable) image.getDrawable()).getBitmap()));
                Imagetraitement im3=btmpactu.teintrapide(0,255,120);
                image.setImageBitmap(im3.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                t1 = System.currentTimeMillis();
                t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);
                return true;

            case R.id.contraster:
                Bitmap btm10= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm10));
                addcolor.setVisibility(View.INVISIBLE);

                btmpactu.affect(btm10); // srx ce new me soul
                seekbar1.setProgress(seekbar1.getMax() / 2);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            if (valeurseek >= 0.5) {
                                valeurseek = (valeurseek - 0.5) * 2;
                                long t0 = System.currentTimeMillis();
                                //image.setImageBitmap(contrastercolor(btmpactu, valeurseek));
                                Imagetraitement im=btmpactu.contrastercolor(valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            } else {
                                valeurseek = valeurseek * 2;
                                long t0 = System.currentTimeMillis();
                               // image.setImageBitmap(reductionhistogramme(btmpactu, (1 - valeurseek)));
                                Imagetraitement im=btmpactu.reductionhistogramme(1-valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;


            case R.id.contrasterteinte:
                Bitmap btm11= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm11));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm11); // srx ce new me soul
                seekbar1.setProgress(seekbar1.getMax() / 2);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            if (valeurseek >= 0.5) {
                                valeurseek = (valeurseek - 0.5) * 2;
                                long t0 = System.currentTimeMillis();
                                //image.setImageBitmap(contrastercolorengardantteinte(btmpactu, valeurseek));
                                Imagetraitement im=btmpactu.contrastercolorengardantteinte(valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            } else {
                                valeurseek = valeurseek * 2;
                                long t0 = System.currentTimeMillis();
                               // image.setImageBitmap(reductionhistogramme(btmpactu, (1 - valeurseek)));
                                Imagetraitement im=btmpactu.reductionhistogramme(1-valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                return true;

            case R.id.contour:
                Bitmap btm12= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm12));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap()); // srx ce new me soul
                //image.setImageBitmap(derive(btmpactu, 0));
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                          //  image.setImageBitmap(derive(contrastercolor(btmpactu, 1), valeurseek));
                            Imagetraitement im=btmpactu.contourderive(valeurseek).contrastercolorengardantteinte(1);
                            image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;

            case R.id.pastel:
                Bitmap btm13= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm13));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm13); // srx ce new me soul
                seekbar1.setProgress(1);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = 1 + (int) (((double) i / seekBar.getMax()) * 254);
                            long t0 = System.currentTimeMillis();
                          //  image.setImageBitmap(pastel(btmpactu, valeurseek));
                            Imagetraitement im=btmpactu.pastel(valeurseek);
                            image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;

            case R.id.grain:
                Bitmap btm14= ((BitmapDrawable) image.getDrawable()).getBitmap();
                btmpactu.liste.addLast(new Imagetraitement(btm14));
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(btm14);
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) (((double) i / seekBar.getMax()) * 255);
                            if (valeurseek != 0) {
                                long t0 = System.currentTimeMillis();
                               // image.setImageBitmap(grain2(btmpactu, valeurseek));
                                Imagetraitement im=btmpactu.grain2(valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;

            case R.id.rotation://mettre à jour position dans imview

                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap());
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        double valeurseek = ((double) i / seekBar.getMax()) * 20;
                        if (fromuser) {
                            if (valeurseek != 0) {
                                int Lw=btmpactu.width;
                                int Lh=btmpactu.height;
                                int lh = (int) (Math.abs(Math.sin(valeurseek)) * Lw + Math.abs(Math.cos(valeurseek)) * Lh) + 1;
                                int lw = (int) (Math.abs(Math.cos(valeurseek)) * Lw + Math.abs(Math.sin(valeurseek)) * Lh) + 1;
                                int decalw = (lw - Lw) / 2;
                                int decalh = (lh - Lh) / 2;
                                image.scrollTo(decalw - btmpactu.posx, decalh - btmpactu.posy);
                                long t0 = System.currentTimeMillis();
                               // image.setImageBitmap(rotation(btmpactu, valeurseek));
                                Imagetraitement im=btmpactu.rotation(valeurseek);
                                image.setImageBitmap(im.afficheuse(btmpactu.width, btmpactu.height, 0,resolution));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;

            case R.id.pastelintelligent:// c'est bcp plus lg depuis que j'ai mis imagetraitement ailleur demander à prof

                addcolor.setVisibility(View.VISIBLE);
                //couleurs = new HashSet<>();
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap());
                //final int[] bitmap = getbitcouleurs(btmpactu);// faut t il passer le tableau, ou faire à chaque fois btm.tab[]//
                final Set<Integer> couleurs=new HashSet<>();// de temps en temps pour des fonctions compliqué, j'ai besoin de set ou d'hashtable.
                final Hashtable<Integer, Imagetraitement.Customclass> table;
                final int[]bitmap=btmpactu.tableauimage;
                final int width = btmpactu.width;
                final int height = btmpactu.height;
                final int size = height * width;
                final Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                zoom.setDensity(resolution);
                //initialisation de la table et récupérat:ion de la moyenne de l'image:
                table = new Hashtable<Integer, Imagetraitement.Customclass>();
                Integer colormax = 0;
                double max = 0;
                for (int i = 0; i < size; i++) {
                    Integer anciennecouleur = (Integer) bitmap[i];
                    if (table.containsKey(anciennecouleur)) {
                        int nbpix = table.get(anciennecouleur).nbpixel;
                        table.get(anciennecouleur).nbpixel += 1;
                        if (nbpix > max) {
                            max = nbpix;
                            colormax = anciennecouleur;
                        }
                    } else {
                        Imagetraitement.Customclass newcustom =  new Imagetraitement().new Customclass();// ainsi j'évite l'erreur not enclosing class( la je creer une instance d'
                        //image non initialisé juste pour accéder à la classe Custom
                        table.put(anciennecouleur, newcustom);
                        couleurs.add(anciennecouleur);
                    }

                }
                int maxb = (colormax & 0x000000FF);
                int maxg = (colormax & 0x0000FF00) >> 8;
                int maxr = (colormax & 0x00FF0000) >> 16;
                //listecouleur = listecouleur.concat("\n(" + maxr + "," + maxg + "," + maxb + ")");
                //System.out.println(maxr + "," + maxg + "," + maxb);
                //on commence par mettre toute l'image a la plus demandée

                for (Integer i : couleurs) {
                    int blui = (i & 0x000000FF);
                    int greeni = (i & 0x0000FF00) >> 8;
                    int redi = (i & 0x00FF0000) >> 16;
                    Imagetraitement.Customclass info = table.get(i);
                    info.projectionr = maxr;
                    info.projectionb = maxb;
                    info.projectiong = maxg;
                    info.distance = sqrt((redi - maxr) * (redi - maxr) + (blui - maxb) * (blui - maxb) + (greeni - maxg) * (greeni - maxg));
                }

                //la table est alors bien initialisé mtn, chaque fois qu on click sur le bouton ajouter une couleur, on anjoute une couleur.
                addcolor.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        System.out.println(couleurs.size());
                        long t0 = System.currentTimeMillis();
                        int[] tab = new int[size];
                        Imagetraitement.intelligentpastelisationtable2pasapas(table, couleurs);
                        for (Integer i = 0; i < size; i++) {
                            int anciennecouleur = bitmap[i];
                            Imagetraitement.Customclass info = table.get(anciennecouleur);
                            int alpha = anciennecouleur >>> 24;
                            tab[i] = (alpha << 24) | (info.projectionr << 16) | (info.projectiong << 8) | info.projectionb;
                            //System.out.println((alpha << 24) | (info.projectionr << 16) | (info.projectiong << 8) | info.projectionb);
                            //System.out.println(bitmap[i]);
                        }
                        zoom.setPixels(tab, 0, width, 0, 0, width, height);
                        long t1 = System.currentTimeMillis();
                        long t2 = t1 - t0;
                        image.setImageBitmap(zoom);
                        txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);

                    }
                });
                return true;



            case R.id.contour2:
                addcolor.setVisibility(View.INVISIBLE);
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap());
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) ((((double) i / seekBar.getMax()) * 100 * sqrt(3)) + 0.5);
                            long t0 = System.currentTimeMillis();
                            //image.setImageBitmap(contrastercolor(contourepidemique(btmpactu, valeurseek), 1));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;

            case R.id.contour3:
                btmpactu.affect(((BitmapDrawable) image.getDrawable()).getBitmap());
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) ((((double) i / seekBar.getMax()) * 100 * sqrt(3)) + 0.5);
                            long t0 = System.currentTimeMillis();
                            //image.setImageBitmap(contrastercolor(contourepidemique2(btmpactu, valeurseek), 1));
                            long t1 = System.currentTimeMillis();
                            long t2 = t1 - t0;
                            txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
                return true;


        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Button addcolor=(Button) findViewById(R.id.addcolor);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            Bitmap bmp = null;
            try {
                bmp = getBitmapFromUri(selectedImage);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            image.setImageBitmap(bmp);
            btmpactu = new Actu(bmp);
            addcolor.setVisibility(View.INVISIBLE);
            image.scrollTo(0,0);
            seekbar1.setVisibility(View.INVISIBLE);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }


    public void onWindowFocusChanged(boolean hasFocus) {// pour avoir accés partout à la taille de la view
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);

        imagewidth = image.getWidth();
        imageheight = image.getHeight();

    }


    // à partir de la, c'est la zone de test. Le brouillon

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public Bitmap contourdeladernierechance(Bitmap b , double seuil){
        int width = b.getWidth();
        int height = b.getHeight();
        int size = height * width;
        int bit[]=new int[size];
        b.getPixels(bit, 0, width, 0, 0, width, height);
        int[] res = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        double tableaudistx[]=new double[(width-1)*(height-1)];
        double tableaudisty[]=new double[(width-1)*(height-1)];
        int i=1;
        int j=1;
        int cibleur=0;
        while (j<size-1){
            if (i >=width-1){
                i=1;
                j+=2;
            }
            int colordroite=bit[j+1];// me vient une idée de ouf ( prendre pour gradienty le max de haut bas, x gauche droite, à tenter plus tard ( peut etre faut les sommer)
            int coloractu=bit[j];
            int colorhaut=bit[j-width];

            int bactu = (coloractu & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gactu = (coloractu >> 8) & 0xFF; //same for the green component
            int ractu = (coloractu >> 16) & 0xFF;//same for the red component

            int bdroite = (colordroite & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gdroite = (colordroite >> 8) & 0xFF; //same for the green component
            int rdroite = (colordroite>> 16) & 0xFF;//same for the red component

            int bhaut = (colorhaut & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int ghaut = (colorhaut >> 8) & 0xFF; //same for the green component
            int rhaut = (colorhaut>> 16) & 0xFF;//same for the red component

            double varx=sqrt((bactu-bdroite)*(bactu-bdroite)+ (gactu-gdroite)*(gactu-gdroite) +(ractu-rdroite)*(ractu-rdroite));//ici plus tard: varx = max haut bas.
            double vary=sqrt((bactu-bhaut)*(bactu-bhaut)+ (gactu-ghaut)*(gactu-ghaut) +(ractu-rhaut)*(ractu-rhaut));

            //on norme(pour faire des différences d'angle), je ne sais pas si il est nécéssaire ici de normer par une euclidienne. à voir plus tard
            double norme= sqrt(varx*varx+vary*vary);
            //double norme=varx+vary;
            varx/=norme;
            vary/=norme;

            tableaudistx[cibleur]=varx;
            tableaudisty[cibleur]=vary;


            cibleur++;
            i++;
            j++;
        }
        //la on a les gradients par pixel.

        return b;

    }



    public Bitmap flourapide(Bitmap b, int intensite) {//on va essayer de pas repasser 800 fois sur le meme pixel.
        // pour cela, on va stocker les sommes sur les lignes en un temps linéaire en widht. Et aprés on itera sur ce nouveau tableau.
        int width = b.getWidth();
        int height = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] tab = new int[size];
        int[] resr = new int[size];
        int[] resg = new int[size];
        int[] resb = new int[size];
        int[]tableaudessommesr=new int[size];
        int[]tableaudessommesb=new int[size];
        int[]tableaudessommesg=new int[size];

        b.getPixels(tab, 0, width, 0, 0, width, height);//Gets the array of the bitmap's pixels
        int color;
        int i;
        int j=0;
        int colorajeter;
        //Log.i(" intensite", " "+ intensite);
        /*for (i=0; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x01;
        }
        for (i=1; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x02;
        }
        for (i=2; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x03;
        }*/


        while (j <size){
            i=0;
            for (int iterateur=j; iterateur< j+intensite+1; iterateur++ ){
                color=tab[iterateur];
                tableaudessommesr[j]+= (color >> 16) & 0xFF;
                tableaudessommesg[j]+= (color >> 8) & 0xFF;
                tableaudessommesb[j]+= color & 0xFF;
                //System.out.println(color & 0xFF);
            }
            j++;
            i++;

            //la on a la somme initiale, va falloir s'occuper de la premiere partie à part, ou on décale vers la droite mais on garde le pixel de gauche
            while (i<=intensite){
                color=tab[j+intensite];
                tableaudessommesr[j]= tableaudessommesr[j-1]+((color >> 16) & 0xFF);
                tableaudessommesg[j]= tableaudessommesg[j-1] + ((color >> 8) & 0xFF);
                tableaudessommesb[j]= tableaudessommesb[j-1]+ (color & 0xFF);

                i++;
                j++;
            }








            //la normalement on a finis les étapes bizarres du début, on itere jsk la prochaine étape bizarre

            while (i<width-intensite){
                color=tab[j+intensite];
                colorajeter=tab[j-intensite-1];

                tableaudessommesr[j]=tableaudessommesr[j-1]+ ((color >> 16) & 0xFF) -((colorajeter >> 16) & 0xFF);
                tableaudessommesg[j]=tableaudessommesg[j-1]+((color >> 8) & 0xFF) - ((colorajeter >> 8) & 0xFF);
                tableaudessommesb[j]=tableaudessommesb[j-1]+ (color & 0xFF) - (colorajeter & 0xFF);
                i++;
                j++;
                //Log.i(" i=", " "+ i);
            }

            /*long t3 = System.currentTimeMillis();
            Log.i(" ligne étape 2", " "+ (t3-t2));*/


            //la faut aller jsk à la fin mais sans ajouter les cases d'aprés

            while (i<width){//-1?
                colorajeter=tab[j-intensite-1];
                tableaudessommesr[j]=tableaudessommesr[j-1] - ((colorajeter >> 16) & 0xFF);
                tableaudessommesg[j]=tableaudessommesg[j-1] - ((colorajeter >> 8) & 0xFF);
                tableaudessommesb[j]=tableaudessommesb[j-1] - (colorajeter & 0xFF);
                i++;
                j++;

            }//bon la j me rend compte qu'y a moyen d'économiser bcp de mémoire si on se passe d'une liste exhaustive de toute les somme pour ensuite faire une post
            // traitement. ça demande d'alterner les phase de récup d'info et de remplissage du tableau final et c 'est trés chaud.
            // une fois qu'on a remplit intensité lignes. On peut commencer à remplire notre tableau final et ainsi décharger la mémoire. à voir.
            /*long t4 = System.currentTimeMillis();
            Log.i(" ligne étape 3", " "+ (t4-t3));*/


        }
        /*for (int t=0; t<size; t++) {
            Log.i(" " + t, " " + tableaudessommesb[t]);
            //tableaudessommesb[t]=1;
        }*/



//COLLONES
        j=0;
        int cpt=0;
        while (cpt<width){
            i=0;
            j=cpt;
            for (int iterateur=j; iterateur< j+width*intensite+1; iterateur+=width ){
                resr[j]+= tableaudessommesr[iterateur];
                resg[j]+= tableaudessommesg[iterateur];
                resb[j]+= tableaudessommesb[iterateur];
            }
            j+=width;
            i+=width;

            //la on a la somme initiale, va falloir s'occuper de la premiere partie à part, ou on décale vers le bas mais on garde le pixel d'en haut.
            while (i<=intensite*width){
                int cibleur=j+intensite*width;
                int red=tableaudessommesr[cibleur];
                int green=tableaudessommesg[cibleur];
                int blu=tableaudessommesb[cibleur];

                resr[j]= resr[j-width]+red;
                resg[j]= resg[j-width] + green;
                resb[j]= resb[j-width] + blu;

                i+=width;
                j+=width;
            }


            //la normalement on a finis les étapes bizarres du début, on itere jsk la prochaine étape bizarre
            while (i<size-intensite*(width+1)){
                int cibleur1=j+intensite*width;
                int cibleur2=j - (intensite+1)*width;
                int red=tableaudessommesr[cibleur1];
                int green=tableaudessommesg[cibleur1];
                int blu=tableaudessommesb[cibleur1];

                int redajeter=tableaudessommesr[cibleur2];
                int greenajeter=tableaudessommesg[cibleur2];
                int bluajeter=tableaudessommesb[cibleur2];

                resr[j]=resr[j-width] + red -redajeter;
                resg[j]=resg[j-width] + green -greenajeter;
                resb[j]=resb[j-width] + blu -bluajeter;

                i+=width;
                j+=width;
            }

                //la faut aller jsk à la fin mais sans ajouter les cases d'en bas

            while (i<=size-width){//-1?
                int cibleur=j - (intensite+1)*width;

                int redajeter=tableaudessommesr[cibleur];
                int greenajeter=tableaudessommesg[cibleur];
                int bluajeter=tableaudessommesb[cibleur];


                resr[j]=resr[j-width] - redajeter;
                resg[j]=resg[j-width] - greenajeter;
                resb[j]=resb[j-width] - bluajeter;
                i+=width;
                j+=width;
            }
            //System.out.println(j + " sizeeeeeeeeeeeeeeeeeeeee= "+ size);
            cpt++;

        }
        /*for (int t=0; t<size; t+=width) {
            //Log.i(" " + t/width, " " + resb[t]);
        }
        for (int t=1; t<size; t+=width) {
           // Log.i(" " + t/width, " " + resb[t]);
        }*/


        double diviseur=0;
        int x=0;
        int y=0;
        for (int jo=0; jo<size;jo++){
            if (x>=width+1){
                x=0;
                y++;
            }
            if(x>=intensite && y>=intensite && (width-1-x)>=intensite && (height-1-y)>=intensite) {
                diviseur=1+2*intensite;
                diviseur*=diviseur;
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
                //System.out.println((double)resr[jo]/diviseur);
            }
            if(x<intensite){
                if(y<intensite){
                    diviseur=(x+intensite+1)*(y+intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
                if((height-1-y)<intensite){
                    diviseur=(x+intensite+1)*((height-1-y)+intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
                if(y>=intensite && (height-1-y)>=intensite){
                    diviseur=(x+intensite+1)*(2*intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
            }
            else if((width-1-x)<intensite){
                if(y<intensite){
                    diviseur=((width-1-x)+intensite+1)*(y+intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
                if((height-1-y)<intensite){
                    diviseur=((width-1-x)+intensite+1)*((height-1-y)+intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
                if(y>=intensite && (height-1-y)>=intensite){
                    diviseur=((width-1-x)+intensite+1)*(2*intensite+1);
                    int a = tab[jo] >>> 24;
                    resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                    x++;
                }
            }
            if(y<intensite && x>=intensite && (width-x)>=intensite){
                diviseur=(2*intensite+1)*(y+intensite+1);
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }
            if((height-1-y)<intensite && x>=intensite && (width-1-x)>=intensite){
                diviseur=(2*intensite+1)*((height-1-y)+intensite+1);
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }
            /*if(x<intensite && y<intensite){
                diviseur=(x*(intensite+1)*(y*(intensite+1)));
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }
            if(y<intensite && (width-x)<intensite){
                diviseur=((width-x)*(intensite+1)*(y*(intensite+1)));
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }
            if(x<intensite && (height-y)<intensite){
                diviseur=(x*(intensite+1)*((height-y)*(intensite+1)));
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }
            if((width-x)<intensite && (height-y)<intensite){
                diviseur=((width-x)*(intensite+1)*((height-y)*(intensite+1)));
                int a = tab[jo] >>> 24;
                resr[jo] = a << 24 | (int) ((resr[jo] / diviseur) + 0.5) << 16 | (int) ((resg[jo] / diviseur) + 0.5) << 8 | (int) ((resb[jo] / diviseur) + 0.5);
                x++;
            }*/

        }
        zoom.setPixels(resr, 0, width, 0, 0, width, height);
        return zoom;
    }



    public Bitmap floumegarapide(Bitmap b, int intensite) {//on va essayer de passer une fois par pixel, et de réduire la complexité en mémoire. C'est méga chaud
        int width = b.getWidth();
        int height = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] tab = new int[size];
        int[] resr = new int[size];
        int[] resg = new int[size];
        int[] resb = new int[size];
        int[]tableaudessommesr=new int[size];
        int[]tableaudessommesb=new int[size];
        int[]tableaudessommesg=new int[size];

        b.getPixels(tab, 0, width, 0, 0, width, height);//Gets the array of the bitmap's pixels
        int color;
        int i;
        int j=0;
        int colorajeter;
        //Log.i(" intensite", " "+ intensite);
        /*for (i=0; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x01;
        }
        for (i=1; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x02;
        }
        for (i=2; i<size; i+=3){
            tab[i]=0xFF << 24 | 0xFF << 16 | (0xFF)<< 8|0x03;
        }*/
        //shéma de l'algo
        //on joue sur la commutativité de l'aadition pour travailler dans le présent, dans le futur et dans le passé et donc ne passer qu'une seule fois par pixel.
        //le présent on lui ajoute la somme. mtn, dans le futur, il faudra enlever cette somme a la case en dessous de intensité cases, et dans
        //le passé, il avait fallu l'ajouter à celle en haut. de plus la case juste en bas commence a cette somme.

        int cpt=0;
        while (j <size){
            i=0;
            int sommer=0,sommeb=0,sommeg=0;//ça c'est la somme horizontal
            for (int iterateur=j; iterateur< j+intensite+1; iterateur++ ){
                color=tab[iterateur];
                sommer+= (color >> 16) & 0xFF;
                sommeg+= (color >> 8) & 0xFF;
                sommeb+= color & 0xFF;
                //System.out.println(color & 0xFF);
            }
            resr[j+width]+=sommer;
            resg[j+width]+=sommeg;
            resb[j+width]+=sommeb;

            resr[j+intensite*width]-=sommer;
            resg[j+intensite*width]-=sommeg;
            resg[j+intensite*width]-=sommeb;

            resr[j-intensite*width]+=sommer;
            resg[j-intensite*width]+=sommeg;
            resg[j-intensite*width]+=sommeb;

            j++;
            i++;

            //la on a la somme initiale, va falloir s'occuper de la premiere partie à part, ou on décale vers la droite mais on garde le pixel de gauche
            while (i<=intensite){
                color=tab[j+intensite];
                sommer+= (color >> 16) & 0xFF;
                sommeg+= (color >> 8) & 0xFF;
                sommeb+= color & 0xFF;




                resr[j]+=sommer;
                resg[j]+=sommeg;
                resb[j]+=sommeb;

                resr[j+width]+=resr[j];
                resg[j+width]+=resg[j];
                resb[j+width]+=resb[j];

                resr[j+intensite*width]-=sommer;//somme ou resb[j] à réfléchir
                resg[j+intensite*width]-=sommeg;
                resg[j+intensite*width]-=sommeb;

                resr[j-intensite*width]+=sommer;//ça c'est pas dit ( pour le bloc)
                resg[j-intensite*width]+=sommeg;
                resg[j-intensite*width]+=sommeb;

                i++;
                j++;
            }




            //la normalement on a finis les étapes bizarres du début, on itere jsk la prochaine étape bizarre

            while (i<width-intensite){
                color=tab[j+intensite];
                sommer+= (color >> 16) & 0xFF;
                sommeg+= (color >> 8) & 0xFF;
                sommeb+= color & 0xFF;


                resr[j]+=sommer;
                resg[j]+=sommeg;
                resb[j]+=sommeb;

                resr[j+intensite*width]-=sommer;
                resg[j+intensite*width]-=sommeg;
                resg[j+intensite*width]-=sommeb;

                resr[j-intensite*width]+=sommer;
                resg[j-intensite*width]+=sommeg;
                resg[j-intensite*width]+=sommeb;

                //tableaudessommesr[j]=tableaudessommesr[j-1]+ ((color >> 16) & 0xFF) -((colorajeter >> 16) & 0xFF);
                //tableaudessommesg[j]=tableaudessommesg[j-1]+((color >> 8) & 0xFF) - ((colorajeter >> 8) & 0xFF);
                //tableaudessommesb[j]=tableaudessommesb[j-1]+ (color & 0xFF) - (colorajeter & 0xFF);
                i++;
                j++;
                //Log.i(" i=", " "+ i);
            }

            /*long t3 = System.currentTimeMillis();
            Log.i(" ligne étape 2", " "+ (t3-t2));*/


            //la faut aller jsk à la fin mais sans ajouter les cases d'aprés

            while (i<width){//-1?
                colorajeter=tab[j-intensite-1];
                tableaudessommesr[j]=tableaudessommesr[j-1] - ((colorajeter >> 16) & 0xFF);
                tableaudessommesg[j]=tableaudessommesg[j-1] - ((colorajeter >> 8) & 0xFF);
                tableaudessommesb[j]=tableaudessommesb[j-1] - (colorajeter & 0xFF);
                i++;
                j++;

            }
            cpt++;//bon la j me rend compte qu'y a moyen d'économiser bcp de mémoire si on se passe d'une liste exhaustive de toute les somme pour ensuite faire une post
            // traitement. ça demande d'alterner les phase de récup d'info et de remplissage du tableau final et c 'est trés chaud.
            // une fois qu'on a remplit intensité lignes. On peut commencer à remplire notre tableau final et ainsi décharger la mémoire. à voir.
            /*long t4 = System.currentTimeMillis();
            Log.i(" ligne étape 3", " "+ (t4-t3));*/


        }
        /*for (int t=0; t<size; t++) {
            Log.i(" " + t, " " + tableaudessommesb[t]);
            //tableaudessommesb[t]=1;
        }*/



        zoom.setPixels(resr, 0, width, 0, 0, width, height);
        return zoom;
    }

}



