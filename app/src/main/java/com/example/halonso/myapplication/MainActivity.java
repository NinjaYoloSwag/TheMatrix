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

    class Position { // je m'en sert pas
        int x;
        int y;

        public Position(int newx, int newy) {
            x = newx;
            y = newy;
        }

        public int hashCode() {
            return x | (y << 8); // * x+ 2^8 *y
        }

        public boolean equals(Object obj) {
            if (obj instanceof Position) {
                return (this.x == x && this.y == y);
            } else {
                return false;
            }
        }
    }

    class Imagetraitement { //pour l'instan j men sert pas

        public Bitmap imagebase;
        //l image sera pas tjr affiché en entier dans l'imageview. Pour gerer ça je garde l'info du pixel du bitmap étant au centre de l'imageview.(pour faire des translations)

        //la y aura un historique des modifications, afin de revenir en arrierre.

        public Imagetraitement(Bitmap b) {
            imagebase = b;
        }

    }

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

    float[][] tabledesdistance;
    ImageView image;
    int imageposx, imageposy;
    int imageheight;
    int imagewidth;
    int resolution;
    TextView txt;
    TextView coordonne;
    TextView nouveaupix;
    Selecteur selecteur;
    SeekBar seekbar1 = null;
    Bitmap btmpactu; // ce bitmap permet de stocké l'image avant un résultat.
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


        selecteur = new

                Selecteur();

        image = (ImageView)

                findViewById(R.id.image);

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
                    //image.scrollTo(x,y);
                    //x dans la bitmap=x-
                    int x = xt - imageposx;
                    int y = yt - imageposy;
                    if (x < widthbitmap && x >= 0 && y >= 0 && y < heightbitmap) {

                        int imview = actu.getPixel(x, y);

                        int newalpha=imview >>> 24;
                        int newr = (imview >> 16) & 0xFF;
                        int newg = (imview >> 8) & 0xFF;
                        int newb = imview & 0xFF;
                        nouveaupix.setText("Apres: (" + newalpha+", " + newr + ", " + newg + ", " + newb + ")");
                    }
                    if (x<btmpactu.getWidth()  && x >= 0 && y >= 0 && y < btmpactu.getHeight() ) {
                        int btmactu = btmpactu.getPixel(x, y);
                        int alphancien = btmactu >>> 24;
                        int rancien = (btmactu >> 16) & 0xFF;
                        int gancien = (btmactu >> 8) & 0xFF;
                        int bancien = btmactu & 0xFF;
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
                    int decalagex = posx - debutx + imageposx - cptfauxmoovex;//vrai position de l'image dans l'écran
                    int decalagey = posy - debuty + imageposy - cptfauxmoovey;

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
                    imageposx += posx - debutx - cptfauxmoovex;
                    imageposy += posy - debuty - cptfauxmoovey;
                }
                return true;

            }
        }


        );
        txt = (TextView)
                findViewById(R.id.txtHello);
        txt.setText(" " + widthphone + " " + heightphone);
        seekbar1 = (SeekBar)
                findViewById(R.id.bar);
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
            tabbitmap[i] = 0xFFFF00FF;

        }
        blanc.setPixels(tabbitmap, 0, 200, 0, 0, 200, 200);

        selecteur.add(blanc, resolution);
        image.setImageBitmap(selecteur.getimagetraiement().imagebase);
        btmpactu = selecteur.getimagetraiement().imagebase;


        final Button adjustdimension = (Button) findViewById(R.id.inside);
        adjustdimension.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long t0 = System.currentTimeMillis();
                Bitmap ancienbitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
                int h = ancienbitmap.getHeight();
                int w = ancienbitmap.getWidth();
                int wview = image.getWidth();
                int hview = image.getHeight();
                double ratiow = (double) wview / w;
                double ratioh = (double) hview / h;
                if (ratiow < ratioh) {
                    Bitmap newbit = redimensionner(ancienbitmap, wview, (int) (ratiow * h + 0.5));
                    image.setImageBitmap(newbit);
                    btmpactu = newbit;
                } else {
                    Bitmap newbit = redimensionner(ancienbitmap, (int) (ratioh * w + 0.5), hview);
                    image.setImageBitmap(newbit);
                    btmpactu = newbit;
                }
                image.scrollTo(0, 0);
                imageposx = 0;
                imageposy = 0;
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);

            }
        });

        final Button next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                long t0 = System.currentTimeMillis();
                seekbar1.setVisibility(View.INVISIBLE);
                //selecteur.getimagetraiement().imagebase=((BitmapDrawable) image.getDrawable()).getBitmap();//on sauvegarde les modifs
                selecteur.next();
                Imagetraitement imageselected = selecteur.getimagetraiement();
                btmpactu = imageselected.imagebase;
                image.setImageBitmap(imageselected.imagebase);
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + image.getHeight() + " " + image.getWidth());
                image.scrollTo(0, 0);
                imageposx = 0;
                imageposy = 0;
            }
        });

        final Button previous = (Button) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                long t0 = System.currentTimeMillis();
                seekbar1.setVisibility(View.INVISIBLE);
                //selecteur.getimagetraiement().imagebase=((BitmapDrawable) image.getDrawable()).getBitmap();
                selecteur.previous();
                Imagetraitement imageselected = selecteur.getimagetraiement();
                image.setImageBitmap(imageselected.imagebase);
                btmpactu = imageselected.imagebase;
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);
                image.scrollTo(0, 0);
                imageposx = 0;
                imageposy = 0;
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
                imageposx = 0;
                imageposy = 0;
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
        switch (item.getItemId()) {
            case R.id.action_save:
                selecteur.add(((BitmapDrawable) image.getDrawable()).getBitmap(), resolution);

                return true;

            case R.id.action_delete:
                selecteur.remove(selecteur.cptactu);
                selecteur.next();
                image.setImageBitmap(selecteur.getimagetraiement().imagebase);
                return true;

            case R.id.redimensionner://mettre à jour postioion dans imview
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(seekbar1.getMax() / 5);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = ((double) i / seekBar.getMax()) * 5;
                            if (valeurseek != 0) {
                                int w = btmpactu.getWidth();
                                int h = btmpactu.getHeight();
                                int newWidth = (int) ((w * valeurseek) + 0.5);
                                int newHeight = (int) ((h * valeurseek) + 0.5);
                                int decalw = (newWidth - w) / 2;
                                int decalh = (newHeight - h) / 2;
                                image.scrollTo(decalw - imageposx, decalh - imageposy);
                                long t0 = System.currentTimeMillis();
                                image.setImageBitmap(redimensionner(btmpactu, (int) ((w * valeurseek) + 0.5), (int) ((h * valeurseek) + 0.5)));
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

            case R.id.griser://200,150,70,0.17)
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                long t0 = System.currentTimeMillis();
                image.setImageBitmap(toGray2(btmpactu));
                long t1 = System.currentTimeMillis();
                long t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);


                return true;

            case R.id.grisersaufteinte://200,150,70,0.17)
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(grisersaufteinte2(btmpactu, 200, 150, 70, 0.17, valeurseek));
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
                t0 = System.currentTimeMillis();
                Bitmap fufu = fusionneri1dansi2(((BitmapDrawable) image.getDrawable()).getBitmap(), ((BitmapDrawable) image.getDrawable()).getBitmap(), 300, 0);
                image.setImageBitmap(fufu);
                t1 = System.currentTimeMillis();
                t2 = t1 - t0;
                txt.setText(fufu.getHeight() + "  " + fufu.getWidth() + " " + t2 + " " + image.getHeight() + " " + image.getWidth());

                return true;

            case R.id.vieux://200,150,70,0.17)
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(vieeux(btmpactu, valeurseek));
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

            case R.id.lum://200,150,70,0.17)
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(seekbar1.getMax() / 10);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = ((double) i / seekBar.getMax() * 10);
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(lum(btmpactu, valeurseek));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) (((double) i / seekBar.getMax() * 50) + 0.5);
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(flou(btmpactu, valeurseek));
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

            case R.id.teinterapide:
                t0 = System.currentTimeMillis();
                image.setImageBitmap(teintrapide(0, 255, 120, ((BitmapDrawable) image.getDrawable()).getBitmap()));
                t1 = System.currentTimeMillis();
                t2 = t1 - t0;
                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth);
                return true;

            case R.id.contraster:

                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
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
                                image.setImageBitmap(contrastercolor(btmpactu, valeurseek));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            } else {
                                valeurseek = valeurseek * 2;
                                long t0 = System.currentTimeMillis();
                                image.setImageBitmap(reductionhistogramme(btmpactu, (1 - valeurseek)));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
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
                                image.setImageBitmap(contrastercolorengardantteinte(btmpactu, valeurseek));
                                long t1 = System.currentTimeMillis();
                                long t2 = t1 - t0;
                                txt.setText(((BitmapDrawable) image.getDrawable()).getBitmap().getHeight() + "  " + ((BitmapDrawable) image.getDrawable()).getBitmap().getWidth() + " " + t2 + " " + imageheight + " " + imagewidth + " " + valeurseek);
                            } else {
                                valeurseek = valeurseek * 2;
                                long t0 = System.currentTimeMillis();
                                image.setImageBitmap(reductionhistogramme(btmpactu, (1 - valeurseek)));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                image.setImageBitmap(derive(btmpactu, 0));
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            double valeurseek = (double) i / seekBar.getMax();
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(derive(contrastercolor(btmpactu, 1), valeurseek));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(1);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = 1 + (int) (((double) i / seekBar.getMax()) * 254);
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(pastel(btmpactu, valeurseek));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) (((double) i / seekBar.getMax()) * 255);
                            if (valeurseek != 0) {
                                long t0 = System.currentTimeMillis();
                                image.setImageBitmap(grain2(btmpactu, valeurseek));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        double valeurseek = ((double) i / seekBar.getMax()) * 180;
                        if (fromuser) {
                            if (valeurseek != 0) {
                                int Lw=btmpactu.getWidth();
                                int Lh=btmpactu.getHeight();
                                double ang = Math.toRadians(valeurseek);
                                int lh = (int) (Math.abs(Math.sin(ang)) * Lw + Math.abs(Math.cos(ang)) * Lh) + 1;
                                int lw = (int) (Math.abs(Math.cos(ang)) * Lw + Math.abs(Math.sin(ang)) * Lh) + 1;
                                int decalw = (lw - Lw) / 2;
                                int decalh = (lh - Lh) / 2;
                                image.scrollTo(decalw - imageposx, decalh - imageposy);
                                long t0 = System.currentTimeMillis();
                                image.setImageBitmap(rotation(btmpactu, valeurseek));
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

            case R.id.pastelintelligent:
                final Button addcolor = (Button) findViewById(R.id.addcolor);
                final Set<Integer> couleurs = new HashSet<>();
                addcolor.setVisibility(View.VISIBLE);
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                final int[] bitmap = getbitcouleurs(btmpactu);
                final int width = btmpactu.getWidth();
                final int height = btmpactu.getHeight();
                final int size = height * width;
                final Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                zoom.setDensity(btmpactu.getDensity());
                //initialisation de la table et récupérat:ion de la moyenne de l'image:
                final Hashtable<Integer, Customclass> table = new Hashtable<Integer, Customclass>();
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
                        Customclass newcustom = new Customclass();
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
                    Customclass info = table.get(i);
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
                        intelligentpastelisationtable2pasapas(table, couleurs);
                        intelligentpastelisationtable2pasapas(table, couleurs);
                        for (Integer i = 0; i < size; i++) {
                            int anciennecouleur = bitmap[i];
                            Customclass info = table.get(anciennecouleur);
                            int alpha = anciennecouleur >> 24;
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) ((((double) i / seekBar.getMax()) * 100 * sqrt(3)) + 0.5);
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(contrastercolor(contourepidemique(btmpactu, valeurseek), 1));
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
                btmpactu = ((BitmapDrawable) image.getDrawable()).getBitmap();
                seekbar1.setProgress(0);
                seekbar1.setVisibility(View.VISIBLE);
                seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int i, boolean fromuser) {
                        if (fromuser) {
                            int valeurseek = (int) ((((double) i / seekBar.getMax()) * 100 * sqrt(3)) + 0.5);
                            long t0 = System.currentTimeMillis();
                            image.setImageBitmap(contrastercolor(contourepidemique2(btmpactu, valeurseek), 1));
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
            btmpactu = bmp;
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

    int[] getbitcouleurs(Bitmap b) {
        int height = b.getHeight();
        int width = b.getWidth();
        int couleurs[] = new int[height * width];
        b.getPixels(couleurs, 0, width, 0, 0, width, height);
        return couleurs;
    }


    public Bitmap lum(Bitmap img, double intensite) {
        int w = img.getWidth();
        int h = img.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        zoom.setDensity(img.getDensity());
        int[] tab = new int[h * w];
        img.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixels
        for (int i = 0; i < w * h; i++) {
            int tmp = tab[i];
            int alpha = (tmp >>> 24);
            int blue = (int) (((tmp & 0xFF) * intensite) + 0.5);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int green = (int) (((tmp >> 8) & 0xFF) * intensite + 0.5); //same for the green component
            int red = (int) (((tmp >> 16) & 0xFF) * intensite + 0.5);//same for the red component
            if (blue > 255) {
                blue = 255;
            }
            if (red > 255) {
                red = 255;
            }
            if (green > 255) {
                green = 255;
            }

            int final_pix = (alpha << 24) | (red << 16) | (green << 8) | blue;//Makes an integer matching the Color's formatting
            tab[i] = final_pix;
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }


    public Bitmap toGray2(Bitmap img) {//tous les credits vont a Maxime <3
        int w = img.getWidth();
        int h = img.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        zoom.setDensity(img.getDensity());
        int[] tab = new int[h * w];
        img.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixels

        for (int i = 0; i < w * h; i++) {
            int tmp = tab[i];
            int a = tmp >> 24;
            int blue = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int green = (tmp >> 8) & 0xFF; //same for the green component
            int red = (tmp >> 16) & 0xFF;//same for the red component
            int max;
            if (blue > red) {
                if (green >= blue) {
                    max = green;
                } else {
                    max = blue;
                }
            } else {
                if (green >= red) {
                    max = green;
                } else {
                    max = red;
                }
            }
            int final_pix = (a << 24) | (max << 16) | (max << 8) | max;//Makes an integer matching the Color's formatting
            tab[i] = final_pix;
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }


    //On voit une teinte comme un triplet de coeff(proportion)(a,b,c) ≤ 1, 1 veut dire que la composante max, c'est la ou il ya le 1 ex:
    //(1,0.2,0,6) veut dire que pour une dose de rouge, y en a 0.6 de bleu et 0.2 de vert. En suite, l'intensité de la teinte c'est le la valeur de l intensité de la
    //valeur rgb max: ex (180, 120, 60) a pour teinte (1,2/3,1/3) et intensité 180.
    // En partant de cela, pour chaque pixel, on récupère l intensité(ou la luminance). et on la multiplie par la teinte voulue. (on remultiplie par l intensité de la teinte voulue/255)
    // pour pouvoir avoir des teintes foncé. (si non quand j lancais avec une teinte genre (255,0,0), sa m faisai (1,0,0)*intensité pixel qui donnait pareil que si j
    //lancais avec (10,0,0)


    public Bitmap teintrapide(int r, int g, int b, Bitmap img) {//juste pour illustrer le fait que sa va 10*plus vite sans ss appel de fonction
        int w = img.getWidth();
        int h = img.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] tab = new int[h * w];
        img.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixel
        int maxe = Math.max(Math.max(r, g), b);
        double gre = (double) g / maxe;
        double blu = (double) b / maxe;
        double re = (double) r / maxe;
        double tata = (double) maxe / 255;
        //double tata=(double)((r+g+b)/3)/255

        for (int i = 0; i < w * h; i++) {
            int tmp = tab[i];
            int alpf = tmp >>> 24;
            int blue = tmp & 0xFF;
            ;
            int green = (tmp >> 8) & 0xFF;
            int red = (tmp >> 16) & 0xFF;
            int lumpix;
            if (blue > red) {
                if (green >= blue) {
                    lumpix = green;
                } else {
                    lumpix = blue;
                }
            } else {
                if (green >= red) {
                    lumpix = green;
                } else {
                    lumpix = red;
                }
            }
            double multiple = lumpix * tata;
            int newr = (int) ((re * multiple) + 0.5);
            int newg = (int) ((gre * multiple) + 0.5);
            int newb = (int) ((blu * multiple) + 0.5);
            tab[i] = Color.argb(alpf, newr, newg, newb);
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }

    public Bitmap teintrapidelegere(Bitmap img, int r, int g, int b, double intensite) {
        int w = img.getWidth();
        int h = img.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] tab = new int[h * w];
        img.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixel
        int maxe = Math.max(Math.max(r, g), b);
        double gre = (double) g / maxe;
        double blu = (double) b / maxe;
        double re = (double) r / maxe;
        double tata = (double) maxe / 255;
        //double tata=(double)((r+g+b)/3)/255

        for (int i = 0; i < w * h; i++) {
            int tmp = tab[i];
            int alpf = tmp >>> 24;
            int blue = tmp & 0xFF;
            ;
            int green = (tmp >> 8) & 0xFF;
            int red = (tmp >> 16) & 0xFF;
            int lumpix;
            if (blue > red) {
                if (green >= blue) {
                    lumpix = green;
                } else {
                    lumpix = blue;
                }
            } else {
                if (green >= red) {
                    lumpix = green;
                } else {
                    lumpix = red;
                }
            }
            double multiple = lumpix * tata;
            double newr = re * multiple;
            double newg = gre * multiple;
            double newb = blu * multiple;

            int moyr = (int) ((intensite * newr + (1 - intensite) * red) + 0.5);
            int moyg = (int) ((intensite * newg + (1 - intensite) * green) + 0.5);
            int moyb = (int) ((intensite * newb + (1 - intensite) * blue) + 0.5);
            tab[i] = Color.argb(alpf, moyr, moyg, moyb);
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }


    Bitmap redimensionner(Bitmap b, int width, int height) {
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int ancienheight = b.getHeight();
        int ancienwidth = b.getWidth();
        int[] couleurs = getbitcouleurs(b);
        int[] nouvellecouleurs = new int[width * height];
        float[][] nouveaurgb = new float[width * height][4];
        double ratiox = (double) width / ancienwidth;
        double ratioy = (double) height / ancienheight;
        double[] tableaux = new double[ancienwidth + 1];
        double[] tableauy = new double[ancienheight + 1];
        for (int i = 0; i < ancienwidth + 1; i++) {
            tableaux[i] = i * ratiox;
        }
        for (int i = 0; i < ancienheight + 1; i++) {
            tableauy[i] = i * ratioy;
        }
        //j viens d me rendre compte qu'on pourrait améliorer et ne pas stocker le tableaux des bords, a faire un jour.
        /*System.out.println(height);
        System.out.println(tableauy[ancienheight]);
        System.out.println(width);
        System.out.println(tableaux[ancienwidth]);*/
        for (int i = 0; i < ancienwidth * ancienheight; i++) {
            int trans = (couleurs[i] >>> 24);
            int red = (couleurs[i] >> 16) & 0xFF;
            int green = (couleurs[i] >> 8) & 0xFF;
            int blue = couleurs[i] & 0xFF;
            /*recupération des bordures.*/
            double gauche = tableaux[i % (ancienwidth)];
            double droite = tableaux[(i % ancienwidth) + 1];
            double haut = tableauy[i / ancienwidth];
            double bas = tableauy[(i / ancienwidth) + 1];
            /*recuperation des pixels potentiellements concerné par i*/
            for (int x = (int) gauche; x <= (int) droite && x < width; x++) {
                for (int y = (int) (haut); y <= (int) bas && y < height; y++) {
                    /*on regarde l'intersection des segmentsx .*/
                    double maxpetitsx;
                    double mingrandsx;
                    if (x >= gauche) {
                        maxpetitsx = x;
                    } else {
                        maxpetitsx = gauche;
                    }
                    if (x + 1 <= droite) {
                        mingrandsx = x + 1;
                    } else {
                        mingrandsx = droite;
                    }
                    double intersectionx = Math.max(0, mingrandsx - maxpetitsx);
                    /*on regarde l'intersection des segmentsy .*/
                    double maxpetitsy;
                    double mingrandsy;
                    if (y >= haut) {
                        maxpetitsy = y;
                    } else {
                        maxpetitsy = haut;
                    }
                    if (y + 1 <= bas) {
                        mingrandsy = y + 1;
                    } else {
                        mingrandsy = bas;
                    }
                    double intersectiony = Math.max(0, mingrandsy - maxpetitsy);
                    /*on a l air de l intersection, le pixel prend les valeurs de i* air .*/
                    double air = intersectionx * intersectiony;
                    nouveaurgb[x + y * width][0] += air * trans;
                    nouveaurgb[x + y * width][1] += air * red;
                    nouveaurgb[x + y * width][2] += air * green;
                    nouveaurgb[x + y * width][3] += air * blue;

                }
            }
        }
       /*on set le btm */
        for (int i = 0; i < width * height; i++) {
            nouvellecouleurs[i] = ((int) (nouveaurgb[i][0] + 0.5) << 24) | ((int) (nouveaurgb[i][1] + 0.5) << 16) | ((int) (nouveaurgb[i][2] + 0.5) << 8) | (int) (nouveaurgb[i][3] + 0.5);
        }
        Bitmap zoom2 = Bitmap.createBitmap(nouvellecouleurs, width, height, Bitmap.Config.ARGB_8888);
        zoom.setPixels(nouvellecouleurs, 0, width, 0, 0, width, height);
        //si je me sert de zoom2 sa marche pas (sa marche, mais si je retouche à l 'image, elle plante)???????

        return zoom;
    }


//pour contraster on pourrait ajouter une intensité en faisant la moyenne entre le pixel du passé et celui aprés filtre, pondéré selon l'intensité du contratse voulu.

    public Bitmap contrastercolor(Bitmap b, double intensite) {
        int cptr = 0;
        int cptg = 0;
        int cptb = 0;
        int height = b.getHeight();
        int width = b.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] red = new int[256];
        int[] green = new int[256];
        int[] blue = new int[256];
        int[] bit = getbitcouleurs(b);
        for (int i : bit) {
            int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (i >> 8) & 0xFF; //same for the green component
            int redi = (i >> 16) & 0xFF;//same for the red component
            red[redi] += 1;
            green[greeni] += 1;
            blue[blui] += 1;
        }
        int[] tabfunctb = new int[256];
        int[] tabfunctr = new int[256];
        int[] tabfunctg = new int[256];
        int sizeb = size - blue[0];
        int sizeg = size - green[0];
        int sizer = size - red[0];
        for (int i = 1; i < 256; i++) {
            // la on commence a 1 psk sinon sa fait des trucs bizarres. (genre quand y a 0 rouge dans la photo, sa sort en rouge, psk le compteur est direct au max et on envoi 255 en rouge
            //à chaque pixels.(l'image sort comme en négatif) là on ne considere pas les pixels nul dans l'histogramme cumulé.
            //plus tard j'aimerais bien faire un truc, genre on modifie proportionnellement nombre d'intensité différente disponible
            cptb += blue[i];
            cptg += green[i];
            cptr += red[i];
            tabfunctb[i] = (int) (((double) cptb / sizeb) * 255 + 0.5);
            tabfunctr[i] = (int) (((double) cptr / sizer) * 255 + 0.5);
            tabfunctg[i] = (int) (((double) cptg / sizeg) * 255 + 0.5);
        }
        //System.out.println(tabfunctr[21]); sa marche aussi quand sizeb for vaut 0. Je suppose que NAN+a=a
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            int moyenneponderer = (int) (((1.0 - intensite) * ri + intensite * tabfunctr[ri]) + 0.5);
            int moyennepondereg = (int) (((1.0 - intensite) * gi + intensite * tabfunctg[gi]) + 0.5);
            int moyennepondereb = (int) (((1.0 - intensite) * bi + intensite * tabfunctb[bi]) + 0.5);
            //bit[i] = (ai << 24) | (tabfunctr[ri] << 16) | (tabfunctg[gi] << 8) | tabfunctb[bi];
            bit[i] = (ai << 24) | (moyenneponderer << 16) | (moyennepondereg << 8) | moyennepondereb;
        }
        zoom.setPixels(bit, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
        return zoom;
    }

    public Bitmap contrastercolorengardantteinte(Bitmap b, double intensite) {
        int cpt = 0;
        int height = b.getHeight();
        int width = b.getWidth();
        int size = height * width;
        int[] lum = new int[256];
        int[] bit = getbitcouleurs(b);
        int max;
        int[] maxes = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int blui = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (tmp >> 8) & 0xFF; //same for the green component
            int redi = (tmp >> 16) & 0xFF;//same for the red component
            //on récupere le max
            if (blui > greeni) {
                if (blui > redi) {
                    max = blui;
                } else {
                    max = redi;
                }

            } else if (redi > greeni) {
                max = redi;
            } else {
                max = greeni;
            }
            lum[max] += 1;
            maxes[i] = max;
        }
        for (int i = 0; i < 256; i++) {
            // la on commence a 1 psk sinon sa fait des trucs bizarres. (genre quand y a 0 rouge dans la photo, sa sort en rouge, psk le compteur est direct au max et on envoi 255 en rouge
            //à chaque pixels.(l'image sort comme en négatif) là on ne considere pas les pixels nul dans l'histogramme cumulé
            cpt += lum[i];
            lum[i] = (int) (((double) cpt / size) * 255 + 0.5);

        }
        for (int i = 0; i < size; i++) {
            //la on chope la teinte du pixel comme précédamment en divisant tout par le max, puis on multiplie par les luminance voulue.
            int tmp = bit[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            int newbi = 0;
            int newgi = 0;
            int newri = 0;
            if (maxes[i] != 0) {
                newbi = (int) (0.5 + ((double) bi / maxes[i]) * lum[maxes[i]]);
                newgi = (int) (0.5 + ((double) gi / maxes[i]) * lum[maxes[i]]);
                newri = (int) (0.5 + ((double) ri / maxes[i]) * lum[maxes[i]]);
            }
            int moyenneponderer = (int) (((1 - intensite) * ri + newri * intensite) + 0.5);
            int moyennepondereg = (int) (((1 - intensite) * gi + newgi * intensite) + 0.5);
            int moyennepondereb = (int) (((1 - intensite) * bi + newbi * intensite) + 0.5);
            bit[i] = (ai << 24) | (moyenneponderer << 16) | (moyennepondereg << 8) | moyennepondereb;

        }
        zoom.setPixels(bit, 0, width, 0, 0, width, height);
        return zoom;
    }


    public Bitmap projectionlineaire(Bitmap b, int a, int c) {//à effacer?
        int height = b.getHeight();
        int width = b.getWidth();
        int size = height * width;
        int[] lum = new int[256];
        int[] bit = getbitcouleurs(b);
        int max;
        int[] maxes = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int blui = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (tmp >> 8) & 0xFF; //same for the green component
            int redi = (tmp >> 16) & 0xFF;//same for the red component
            //on récupere le max
            if (blui > greeni) {
                if (blui > redi) {
                    max = blui;
                } else {
                    max = redi;
                }

            } else if (redi > greeni) {
                max = redi;
            } else {
                max = greeni;
            }
            maxes[i] = max;
        }
        for (int i = 0; i < 256; i++) {

            lum[i] = (int) (0.5 + a + (c - a) * ((double) i / 255.0));
        }
        for (int i = 0; i < size; i++) {
            //la on chope la teinte du pixel comme précédamment en divisant tout par le max, puis on multiplie par les luminance voulue.
            int tmp = bit[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            int newbi = 0;
            int newri = 0;
            int newgi = 0;
            if (maxes[i] != 0) {
                newbi = (int) (0.5 + ((double) bi / maxes[i]) * lum[maxes[i]]);
                newgi = (int) (0.5 + ((double) gi / maxes[i]) * lum[maxes[i]]);
                newri = (int) (0.5 + ((double) ri / maxes[i]) * lum[maxes[i]]);
            }
            bit[i] = (ai << 24) | (newri << 16) | (newgi << 8) | newbi;

        }
        zoom.setPixels(bit, 0, width, 0, 0, width, height);
        return zoom;
    }

    public Bitmap reductionhistogramme(Bitmap b, double intensite) {
        int height = b.getHeight();
        int width = b.getWidth();
        int size = height * width;
        int[] lum = new int[256];
        int[] bit = getbitcouleurs(b);
        int max;
        int min = 256;
        int[] maxes = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int blui = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (tmp >> 8) & 0xFF; //same for the green component
            int redi = (tmp >> 16) & 0xFF;//same for the red component
            //on récupere le max
            if (blui > greeni) {
                if (blui > redi) {
                    max = blui;
                } else {
                    max = redi;
                }

            } else if (redi > greeni) {
                max = redi;
            } else {
                max = greeni;
            }
            maxes[i] = max;
            if (max < min) {//on recupe la plus petite lum non nul de l'histo
                min = max;
            }
        }
        for (int i = min; i < 256; i++) {

            lum[i] = (int) (0.5 + min + (255 * (1 - intensite) - min) * ((double) (i - min) / (255 - min)));
        }
        for (int i = 0; i < size; i++) {
            //la on chope la teinte du pixel comme précédamment en divisant tout par le max, puis on multiplie par les luminance voulue.
            int tmp = bit[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            int newbi = 0;
            int newri = 0;
            int newgi = 0;
            if (maxes[i] != 0) {
                newbi = (int) (0.5 + ((double) bi / maxes[i]) * lum[maxes[i]]);
                newgi = (int) (0.5 + ((double) gi / maxes[i]) * lum[maxes[i]]);
                newri = (int) (0.5 + ((double) ri / maxes[i]) * lum[maxes[i]]);
            }
            bit[i] = (ai << 24) | (newri << 16) | (newgi << 8) | newbi;

        }
        zoom.setPixels(bit, 0, width, 0, 0, width, height);
        return zoom;
    }


    public Bitmap contrastercolor3(Bitmap b, double intensite) {//chelou peu etre buggé à effacer?
        int cptr = 0;
        int cptg = 0;
        int cptb = 0;
        int nbteinter = -1;//si il n' ya qu'une seule teinte de bleu, le contraste sur le champ bleu n' as pas de sens, donc on met -1, bam;
        int nbteinteg = -1;
        int nbteinteb = -1;
        int height = b.getHeight();
        int width = b.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] red = new int[256];
        int[] green = new int[256];
        int[] blue = new int[256];
        int[] bit = getbitcouleurs(b);
        for (int i : bit) {
            int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (i >> 8) & 0xFF; //same for the green component
            int redi = (i >> 16) & 0xFF;//same for the red component
            if (red[redi] == 0) {
                nbteinter += 1;
            }
            if (green[greeni] == 0) {
                nbteinteg += 1;
            }
            if (blue[blui] == 0) {
                nbteinteb += 1;
            }
            red[redi] += 1;
            green[greeni] += 1;
            blue[blui] += 1;

        }
        int[] tabfunctb = new int[256];
        int[] tabfunctr = new int[256];
        int[] tabfunctg = new int[256];
        int sizeb = size - blue[0];
        int sizeg = size - green[0];
        int sizer = size - red[0];
        for (int i = 1; i < 256; i++) {
            // la on commence a 1 psk sinon sa fait des trucs bizarres. (genre quand y a 0 rouge dans la photo, sa sort en rouge, psk le compteur est direct au max et on envoi 255 en rouge
            //à chaque pixels.(l'image sort comme en négatif) là on ne considere pas les pixels nul dans l'histogramme cumulé.
            //plus tard j'aimerais bien faire un truc, genre on modifie proportionnellement nombre d'intensité différente disponible

            cptb += blue[i];
            cptg += green[i];
            cptr += red[i];
            tabfunctb[i] = (int) (((double) cptb / sizeb) * 255 + 0.5);
            tabfunctr[i] = (int) (((double) cptr / sizer) * 255 + 0.5);
            tabfunctg[i] = (int) (((double) cptg / sizeg) * 255 + 0.5);
        }
        //System.out.println(tabfunctr[21]); sa marche aussi quand sizeb for vaut 0. Je suppose que NAN+a=a
        if (nbteinteb == -1) {
            nbteinteb = 0;
        }
        if (nbteinteg == -1) {
            nbteinteg = 0;
        }
        if (nbteinter == -1) {
            nbteinter = 0;
        }
        //recup du nb teinte maxe
        int max;
        if (nbteinteb < nbteinteg) {
            if (nbteinteg < nbteinter) {
                max = nbteinter;
            } else {
                max = nbteinteg;
            }
        } else if (nbteinteb > nbteinter) {
            max = nbteinteb;
        } else {
            max = nbteinter;
        }
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            double conservationcouleurr = (double) (nbteinter / max);// si y'a pas bcp de teinte différente, le contraste n'a pas de sens, c'est ce qui fait que parfois l'égalistaion modifie les couleurs. Du coup moins y'a de teinte bleu, plus on bride l'effet d'égalisation bleu.
            double conservationcouleurg = (double) (nbteinteg / max);
            double conservationcouleurb = (double) (nbteinteb / max);

            int newr = (int) ((conservationcouleurr * tabfunctr[ri] + (1 - conservationcouleurr) * ri) + 0.5);
            double ratior = (double) newr / ri;
            int g1 = (int) ((ratior * gi) + 0.5);
            int b1 = (int) ((ratior * bi) + 0.5);
            int newg = (int) ((conservationcouleurg * tabfunctg[gi] + (1 - conservationcouleurg) * gi) + 0.5);
            double ratiog = (double) newg / gi;
            int r1 = (int) ((ratiog * ri) + 0.5);
            int b2 = (int) ((ratiog * bi) + 0.5);
            int newb = (int) ((conservationcouleurb * tabfunctb[bi] + (1 - conservationcouleurb) * bi) + 0.5);
            double ratiob = (double) newb / bi;
            int r2 = (int) ((ratiob * ri) + 0.5);
            int g2 = (int) ((ratiob * gi) + 0.5);
            int resr = (int) ((conservationcouleurr * newr + conservationcouleurg * r1 + conservationcouleurb * r2) + 0.5);
            int resg = (int) ((conservationcouleurr * g1 + conservationcouleurg * newg + conservationcouleurb * g2) + 0.5);
            int resb = (int) ((conservationcouleurr * b1 + conservationcouleurg * b2 + conservationcouleurb * newb) + 0.5);

            int moyenneponderer = (int) (((1.0 - intensite) * ri + intensite * newr) + 0.5);
            int moyennepondereg = (int) (((1.0 - intensite) * gi + intensite * newg) + 0.5);
            int moyennepondereb = (int) (((1.0 - intensite) * bi + intensite * newb) + 0.5);

            //bit[i] = (ai << 24) | (tabfunctr[ri] << 16) | (tabfunctg[gi] << 8) | tabfunctb[bi];
            bit[i] = (ai << 24) | (moyenneponderer << 16) | (moyennepondereg << 8) | moyennepondereb;
        }
        zoom.setPixels(bit, 0, width, 0, 0, width, height);
        return zoom;
    }


    public Bitmap grisersaufteinte2(Bitmap btm, int r, int g, int b, double precision, double intensite) {

        double seuil = precision * precision * 255 * 255 * 3;
        int[] couleurs = getbitcouleurs(btm);
        int height = btm.getHeight();
        int width = btm.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(btm.getDensity());
        for (int i = 0; i < height * width; i++) {
            int tmp = couleurs[i];
            int ai = (tmp >>> 24);
            int bi = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int gi = (tmp >> 8) & 0xFF; //same for the green component
            int ri = (tmp >> 16) & 0xFF;//same for the red component
            int maxe = 0;
            if (ri > maxe) {
                maxe = ri;
            }
            if (gi > maxe) {
                maxe = gi;
            }
            if (bi > maxe) {
                maxe = bi;
            }
            double diffr = r - ri;
            double diffg = g - gi;
            double diffb = b - bi;
            if (diffr < 0) {
                diffr = -diffr;
            }
            if (diffg < 0) {
                diffg = -diffg;
            }
            if (diffb < 0) {
                diffb = -diffb;
            }
            //double distance=(double)1/(2*somme)*((r+ri)*diffr+(g+gi)*diffg+(b+bi)*diffb);
            //double distance=sqrt(diffr*diffr+diffb*diffb+diffg*diffg);
            //double distance=
            int distance = (ri - r) * (ri - r) + (gi - g) * (gi - g) + (bi - b) * (bi - b);
            if (distance > seuil) {
                int moyenneponderer = (int) ((intensite * maxe + ri * (1 - intensite)) + 0.5);
                int moyennepondereg = (int) ((intensite * maxe + gi * (1 - intensite)) + 0.5);
                int moyennepondereb = (int) ((intensite * maxe + bi * (1 - intensite)) + 0.5);
                couleurs[i] = (ai << 24) | (moyenneponderer << 16) | (moyennepondereg << 8) | moyennepondereb;
            } else {
                couleurs[i] = tmp;
            }
        }
        zoom.setPixels(couleurs, 0, width, 0, 0, width, height);
        return zoom;
    }


    public Bitmap fusionneri1dansi2(Bitmap derriere, Bitmap devant, int x, int y) {// bizarre gestion alpha
        int[] couleursderrieres = getbitcouleurs(derriere);
        int[] couleurdevant = getbitcouleurs(devant);
        int derw = derriere.getWidth();
        int devw = devant.getWidth();
        int derh = derriere.getHeight();
        int devh = devant.getHeight();
        int abscissederierenouvellebase;
        int width;
        int ordonnederierenouvellebase;
        int height;
        int absciseedevantnouvellebase;
        int ordonnedevantnouvellebase;
        if (x < 0) {
            abscissederierenouvellebase = -x;
            width = -x + derw;
            absciseedevantnouvellebase = 0;
        } else {
            abscissederierenouvellebase = 0;
            width = x + devw;
            absciseedevantnouvellebase = x;

        }
        if (y > 0) {
            ordonnederierenouvellebase = y;
            height = y + derh;
            ordonnedevantnouvellebase = 0;
        } else {
            ordonnederierenouvellebase = 0;
            height = -y + devh;
            ordonnedevantnouvellebase = y;
        }
        int[] fusion = new int[width * height];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(Bitmap.DENSITY_NONE);
        //on peint l'arriere
        for (int i = abscissederierenouvellebase; i < abscissederierenouvellebase + derw; i++) {
            for (int j = ordonnederierenouvellebase; j < ordonnederierenouvellebase + derh; j++) {
                fusion[j * width + i] = couleursderrieres[(j - ordonnederierenouvellebase) * derw + i - abscissederierenouvellebase];
            }
        }
        //on peint l'avant, mais en controlant avec alpha
        for (int i = absciseedevantnouvellebase; i < absciseedevantnouvellebase + devw; i++) {
            for (int j = ordonnedevantnouvellebase; j < ordonnedevantnouvellebase + devh; j++) {
                int colordevant = couleurdevant[(j - ordonnedevantnouvellebase) * devw + i - absciseedevantnouvellebase];
                int alpha = colordevant >>> 24;
                int colorderriere = fusion[j * width + i];
                if (alpha != 255) {
                    double ratio = alpha / 255.0;
                    int alphaderriere = (colorderriere >>> 24);
                    int bderriere = (colorderriere & 0xFF);
                    int gderriere = (colorderriere >> 8) & 0xFF;
                    int rderriere = (colorderriere >> 16) & 0xFF;


                    int bdevant = (colordevant & 0xFF);
                    int gdevant = (colordevant >> 8) & 0xFF;
                    int rdevant = (colordevant >> 16) & 0xFF;

                    int newalpha;

                    if (alphaderriere > alpha){
                        newalpha= alphaderriere;
                    }
                    else{
                        newalpha=alpha;
                    }

                    int newr = (int) (((1 - ratio) * rderriere + ratio * rdevant) + 0.5);
                    int newg = (int) (((1-ratio) * gderriere + ratio * gdevant) + 0.5);
                    int newb = (int) (((1-ratio) * bderriere + ratio * bdevant) + 0.5);
                    fusion[j * width + i] = (newalpha << 24) | (newr << 16) | (newg << 8) | newb;
                }

                else {
                    fusion[j * width + i] = colordevant;
                }

            }
        }
        //Bitmap img=Bitmap.createBitmap(fusion,width, height, Bitmap.Config.ARGB_8888);
        zoom.setPixels(fusion, 0, width, 0, 0, width, height);
        return zoom;

    }


    public Bitmap derive(Bitmap btm, double seuil) {// à effacer?
        seuil *= 255;
        int height = btm.getHeight();
        int width = btm.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(btm.getDensity());
        int size = height * width;
        int[] bit = getbitcouleurs(btm);
        int[] tab = new int[size];
        for (int i = 1; i < width - 1; i++) {
            for (int j = 1; j < height - 1; j++) {
                int color = bit[i + width * j];
                int alpha = (color) >> 24;
                int blui = (color & 0x000000FF);
                int greeni = (color & 0x0000FF00) >> 8;
                int redi = (color & 0x00FF0000) >> 16;
                double res = 0;
                //recup des bas/
                for (int x = i - 1; x <= i + 1; x++) {
                    int colorvoisin = bit[x + width * (j - 1)];
                    int bluivoisin = (colorvoisin & 0x000000FF);
                    int greenivoisin = (colorvoisin & 0x0000FF00) >> 8;
                    int redivoisin = (colorvoisin & 0x00FF0000) >> 16;
                    res += sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin));
                }
                //recupdes droits
                for (int x = j - 1; x <= j + 1; x++) {
                    int colorvoisin = bit[i + 1 + width * x];
                    int bluivoisin = (colorvoisin & 0x000000FF);
                    int greenivoisin = (colorvoisin & 0x0000FF00) >> 8;
                    int redivoisin = (colorvoisin & 0x00FF0000) >> 16;
                    res += sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin));
                }

                res = res / 4;
                int resu = (int) ((res / sqrt(3) + 0.5));
                if (resu < seuil) {
                    tab[i + width * j] = (alpha << 24) | 0x00000000;
                } else {
                    tab[i + width * j] = (alpha << 24) | (resu << 16) | (resu << 8) | resu;
                    //tab[i + width * j] = 0xFFFFFFFF;
                }
            }

        }
        zoom.setPixels(tab, 0, width, 0, 0, width, height);
        return contrastercolorengardantteinte(zoom, 1);
    }


    public Bitmap derive2(Bitmap btm, double seuil) { //à effacer?
        seuil *= 255.0;
        int height = btm.getHeight();
        int width = btm.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(btm.getDensity());
        int size = height * width;
        int[] bit = getbitcouleurs(btm);
        int[] tab = new int[size];
        for (int i = 1; i < width - 1; i++) {
            for (int j = 1; j < height - 1; j++) {
                int color = bit[i + width * j];
                int alpha = (color) >> 24;
                int blui = (color & 0x000000FF);
                int greeni = (color & 0x0000FF00) >> 8;
                int redi = (color & 0x00FF0000) >> 16;
                int res = 0;
                double max = 0;
                //recup des bas
                for (int x = i - 1; x <= i; x++) {
                    int colorvoisin = bit[x + width * (j - 1)];
                    int bluivoisin = (colorvoisin & 0x000000FF);
                    int greenivoisin = (colorvoisin & 0x0000FF00) >> 8;
                    int redivoisin = (colorvoisin & 0x00FF0000) >> 16;
                    res = (int) (sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin)) + 0.5);
                }
                if (res > max) {
                    max = res;
                }
                //recupdes droits
                for (int x = j; x <= j + 1; x++) {
                    int colorvoisin = bit[i + 1 + width * x];
                    int bluivoisin = (colorvoisin & 0x000000FF);
                    int greenivoisin = (colorvoisin & 0x0000FF00) >> 8;
                    int redivoisin = (colorvoisin & 0x00FF0000) >> 16;
                    res = (int) (sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin)) + 0.5);
                    if (res > max) {
                        max = res;
                    }
                }
                max = max / sqrt(3);
                int maxe = (int) (max + 0.5);
                if (maxe <= seuil) {
                    tab[i + width * j] = (alpha << 24) | 0;
                } else {
                    tab[i + width * j] = (alpha << 24) | (maxe << 16) | (maxe << 8) | maxe;
                    //tab[i + width * j] = 0xFFFFFFFF;
                }
            }

        }
        zoom.setPixels(tab, 0, width, 0, 0, width, height);
        return contrastercolorengardantteinte(zoom, 1);
    }

    public Bitmap derive3(Bitmap btm, double seuil) { // à effacer ?
        seuil *= 255.0;
        double racine3 = sqrt(3);
        int height = btm.getHeight();
        int width = btm.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(btm.getDensity());
        int size = height * width;
        int[] bit = getbitcouleurs(btm);
        int[] tab = new int[size];
        for (int i = 1; i < width - 1; i++) {
            for (int j = 1; j < height - 1; j++) {
                int color = bit[i + width * j];
                int alpha = (color) >> 24;
                int blui = (color & 0x000000FF);
                int greeni = (color & 0x0000FF00) >> 8;
                int redi = (color & 0x00FF0000) >> 16;
                int res = 0;
                double max = 0;
                //recup des bas, on multiplira tout sa par un indice de ressemblance (des 3 a droite / bas), pour limiter les pixels parisites.
                int colorvoisin = bit[i - 1 + width * (j - 1)];
                int bluivoisin = (colorvoisin & 0x000000FF);
                int greenivoisin = (colorvoisin & 0x0000FF00) >> 8;
                int redivoisin = (colorvoisin & 0x00FF0000) >> 16;

                double distancevoisinsgauchebas = sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin));

                int colorvoisin2 = bit[i + width * (j - 1)];
                int bluivoisin2 = (colorvoisin2 & 0x000000FF);
                int greenivoisin2 = (colorvoisin2 & 0x0000FF00) >> 8;
                int redivoisin2 = (colorvoisin2 & 0x00FF0000) >> 16;

                double distancevoisinsbas = sqrt((blui - bluivoisin) * (blui - bluivoisin) + (redi - redivoisin) * (redi - redivoisin) + (greeni - greenivoisin) * (greeni - greenivoisin));

                double moyennebas = ((distancevoisinsbas + distancevoisinsgauchebas) / racine3) / 2;//on divise parsqrt 3 pour ramener les distances dans 0 255;
                double distancedesvoisinsbas = (sqrt((bluivoisin2 - bluivoisin) * (bluivoisin2 - bluivoisin) + (redivoisin2 - redivoisin) * (redivoisin2 - redivoisin) + (greenivoisin2 - greenivoisin) * (greenivoisin2 - greenivoisin))) / (255 * racine3);
                //on divise cette distance pour ramener dans l'intervalle 0 1//faudrapeut etre ramener a 0.5 vu qu on somme les derivées partielles
                double bas = (1 - distancedesvoisinsbas) * moyennebas;

                //recup du cote droit
                int colorvoisin3 = bit[i + 1 + width * (j + 1)];
                int bluivoisin3 = (colorvoisin3 & 0x000000FF);
                int greenivoisin3 = (colorvoisin3 & 0x0000FF00) >> 8;
                int redivoisin3 = (colorvoisin3 & 0x00FF0000) >> 16;

                double distancevoisinshautdroit = sqrt((blui - bluivoisin3) * (blui - bluivoisin3) + (redi - redivoisin3) * (redi - redivoisin3) + (greeni - greenivoisin3) * (greeni - greenivoisin3));

                int colorvoisin4 = bit[i + 1 + width * j];
                int bluivoisin4 = (colorvoisin4 & 0x000000FF);
                int greenivoisin4 = (colorvoisin4 & 0x0000FF00) >> 8;
                int redivoisin4 = (colorvoisin4 & 0x00FF0000) >> 16;

                double distancevoisinsdroit = sqrt((blui - bluivoisin4) * (blui - bluivoisin4) + (redi - redivoisin4) * (redi - redivoisin4) + (greeni - greenivoisin4) * (greeni - greenivoisin4));

                double moyennedroite = ((distancevoisinsdroit + distancevoisinshautdroit) / racine3) / 2;
                double distancedesvoisinsdroits = (sqrt((bluivoisin4 - bluivoisin3) * (bluivoisin4 - bluivoisin3) + (redivoisin4 - redivoisin3) * (redivoisin4 - redivoisin3) + (greenivoisin4 - greenivoisin3) * (greenivoisin4 - greenivoisin3))) / (255 * racine3);
                double droit = (1 - distancedesvoisinsdroits) * moyennedroite;

                int resultat = (int) (droit + bas + 0.5);
                if (resultat <= seuil) {
                    tab[i + width * j] = (alpha << 24) | 0;
                } else {
                    tab[i + width * j] = (alpha << 24) | (resultat << 16) | (resultat << 8) | resultat;
                    //tab[i + width * j] = 0xFFFFFFFF;
                }
            }

        }
        zoom.setPixels(tab, 0, width, 0, 0, width, height);
        return contrastercolorengardantteinte(zoom, 1);
    }


    public Bitmap pastel(Bitmap btm, int intensite) {// à faire: au lieu de regarder l ensemble (n/intensitéZ)^3, faudrait creer l'ensemble des couleurs les plus presentes dans l'image.
        int height = btm.getHeight();
        int width = btm.getWidth();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] cache = new int[256];
        zoom.setDensity(btm.getDensity());// à faire : remplacer tout les getdensity par density ecran
        int size = height * width;
        int[] bit = getbitcouleurs(btm);
        for (int i = 0; i < size; i++) {
            int tmp = bit[i];
            int alpha = tmp >>> 24;
            int blui = (tmp & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (tmp >> 8) & 0xFF; //same for the green component
            int redi = (tmp >> 16) & 0xFF;//same for the red component
            int resb;
            int resg;
            int resr;
            if (blui != 0 && cache[blui] == 0) {
                int resteb = blui % intensite;
                if (resteb < intensite / 2) {
                    cache[blui] = blui - resteb;

                } else {
                    int aux = blui + intensite - resteb;
                    if (aux <= 255) {
                        cache[blui] = aux;
                    } else {
                        cache[blui] = blui - resteb;
                    }
                }
            }
            if (greeni != 0 && cache[greeni] == 0) {
                int resteg = greeni % intensite;
                if (resteg < intensite / 2) {
                    cache[greeni] = greeni - resteg;
                } else {
                    int aux = greeni + intensite - resteg;
                    if (aux <= 255) {
                        cache[greeni] = aux;
                    } else {
                        cache[greeni] = greeni - resteg;
                    }
                }
            }
            if (redi != 0 && cache[redi] == 0) {
                int rester = redi % intensite;
                if (rester < intensite / 2) {
                    cache[redi] = redi - rester;
                } else {
                    int aux = redi + intensite - rester;
                    if (aux <= 255) {
                        cache[redi] = aux;
                    } else {
                        cache[redi] = redi - rester;
                    }
                }
            }
            bit[i] = (alpha << 24) | (cache[redi] << 16) | (cache[greeni] << 8) | cache[blui];
        }
        zoom.setPixels(bit, 0, width, 0, 0, width, height);
        return zoom;
    }


    // il me faut une structure de donnée  : hashtable couleurs-> {nbdepixeldecettecouleur,projectioncouleuractuelle,distancedelaprojection->originale}
    class Customclass {//je m'en sert
        int nbpixel;
        int projectionr;
        int projectiong;
        int projectionb;
        double distance;

        public Customclass() {
            nbpixel = 1;
            projectionr = 0;
            projectiong = 0;
            projectionb = 0;
        }


    }

    public Bitmap intelligentpastelisationtable2(Bitmap b, int nbcolors) {//pareil que celle qui prend un hashtabl en entrée mais elle elle oneshoot.
        String listecouleur = " ";
        // à l'image de base n fois
        int[] bitmap = getbitcouleurs(b);
        int width = b.getWidth();
        int height = b.getHeight();
        int size = height * width;
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        //initialisation de la table et récupérat:ion de la moyenne de l'image:
        Hashtable<Integer, Customclass> table = new Hashtable<Integer, Customclass>();
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
                Customclass newcustom = new Customclass();
                table.put(anciennecouleur, newcustom);
            }

        }

        int maxb = (colormax & 0xFF);
        int maxg = (colormax >> 8) & 0xFF;
        int maxr = (colormax >> 16) & 0xFF;
        listecouleur = listecouleur.concat("\n(" + maxr + "," + maxg + "," + maxb + ")");
        //System.out.println(listecouleur);
        //on commence par mettre toute l'image a la plus demandée
        Set<Integer> couleurs = table.keySet();
        for (Integer i : couleurs) {
            int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (i >> 8) & 0xFF; //same for the green component
            int redi = (i >> 16) & 0xFF;//same for the red component
            Customclass info = table.get(i);
            info.projectionr = maxr;
            info.projectionb = maxb;
            info.projectiong = maxg;
            info.distance = sqrt((redi - maxr) * (redi - maxr) + (blui - maxb) * (blui - maxb) + (greeni - maxg) * (greeni - maxg));
        }
        //la table est alors bien initialisé
        //System.out.println(table);

        for (int cpt = 0; cpt < nbcolors - 1; cpt++) {// recupération max
            max = 0;
            colormax = 0;
            for (Integer i : couleurs) {
                int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
                int greeni = (i >> 8) & 0xFF; //same for the green component
                int redi = (i >> 16) & 0xFF;//same for the red component
                Customclass info = table.get(i);
                int projectr = info.projectionr;
                int projectg = info.projectiong;
                int projectb = info.projectionb;

                double d = sqrt(((redi - projectr) * (redi - projectr)) + ((greeni - projectg) * (greeni - projectg)) + ((blui - projectb) * (blui - projectb))); // la on a la distance entre le pixel et sa valeur d'antan
                //System.out.println(d);
                //System.out.println("("+redi +"," +greeni+","+ blui+")");
                //System.out.println(info.nbpixel);
                //System.out.println(d);
                double nbpixfoisd = d * info.nbpixel;
                if (nbpixfoisd > max) {
                    max = nbpixfoisd;
                    colormax = i;
                }
            }
            //System.out.println(colormax);

            if (max == 0) {// si tout est à nul, on peut pas faire mieux, on arrette et on set le pixel
                for (Integer i = 0; i < size; i++) {
                    Customclass info = table.get(bitmap[i]);
                    int alpha = (bitmap[i]) >> 24;
                    bitmap[i] = (alpha << 24) | (info.projectionr << 16) | (info.projectiong << 8) | info.projectionb;
                }
                zoom.setPixels(bitmap, 0, width, 0, 0, width, height);
                return zoom;
            }
            int newblue = (colormax & 0xFF);
            int newgreen = (colormax >> 8) & 0xFF;
            int newred = (colormax >> 16) & 0xFF;
            listecouleur = listecouleur.concat("\n(" + newred + "," + newgreen + "," + newblue + ")");
            //System.out.println(listecouleur);
            for (Integer i : couleurs) {// mise a jour de la table en fonction de la nouvelle couleur
                Customclass info = table.get(i);
                int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
                int greeni = (i >> 8) & 0xFF; //same for the green component
                int redi = (i >> 16) & 0xFF;//same for the red component
                double anciennedistance = info.distance;
                double nouvelledistance = sqrt(((redi - newred) * (redi - newred) + (blui - newblue) * (blui - newblue) + (greeni - newgreen) * (greeni - newgreen)));
                if (nouvelledistance < anciennedistance) {//modification de ma custom classe à partir de ma hashtable(marche bien)
                    info.distance = nouvelledistance;
                    info.projectionr = newred;
                    info.projectiong = newgreen;
                    info.projectionb = newblue;
                    //System.out.println(table.get(i).projectionb);
                    //System.out.println(newblue);
                }
            }
        }
        System.out.println(listecouleur);
        //à la fin, on se sert de klat table pour construire le tableau bitmap
        for (Integer i = 0; i < size; i++) {
            int anciennecouleur = bitmap[i];
            Customclass info = table.get(anciennecouleur);
            int alpha = anciennecouleur >> 24;
            bitmap[i] = (alpha << 24) | (info.projectionr << 16) | (info.projectiong << 8) | info.projectionb;
        }
        zoom.setPixels(bitmap, 0, width, 0, 0, width, height);
        return zoom;
    }

    public Hashtable<Integer, Customclass> intelligentpastelisationtable2pasapas(Hashtable<Integer, Customclass> table, Set<Integer> couleurs) {// bon y a quand meme un probleme,
        // c'est que l'ordre de selection correspond pas vraiment à ce que ferait un humain. Le probleme c'est qu'il faudrait que chaque couleur
        //appel aussi les couleurs voisines mais avec des coefs moins important. ( à faire)


        // l'idée est la suivante, pour chaque couleur originale presente dans l'image, on regarde la distance entre elle et sa projeté actuelle.
        // Si c'est gros et si elle represente bcp de pixel, elle va bcp peser pour le choix de la nouvelle couleurs que l'on va ajouter.
        // une fois qu on a la nouvelle projection, on met à jour tt les autres couleurs(si cette projection est plus proche que l'ancienne, on la garde)


        //initialisation de la table et récupérat:ion de la moyenne de l'image:
        // recupération max
        double max = 0;
        Integer colormax = 0;
        for (Integer i : couleurs) {
            int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (i >> 8) & 0xFF; //same for the green component
            int redi = (i >> 16) & 0xFF;//same for the red component
            Customclass info = table.get(i);
            int projectr = info.projectionr;
            int projectg = info.projectiong;
            int projectb = info.projectionb;

            double d = sqrt(((redi - projectr) * (redi - projectr)) + ((greeni - projectg) * (greeni - projectg)) + ((blui - projectb) * (blui - projectb))); // la on a la distance entre le pixel et sa valeur d'antan
            //System.out.println(d);
            //System.out.println("("+redi +"," +greeni+","+ blui+")");
            //System.out.println(info.nbpixel);
            //System.out.println(d);
            double nbpixfoisd = d * info.nbpixel;
            if (nbpixfoisd > max) {
                max = nbpixfoisd;
                colormax = i;
            }
        }
        //System.out.println(colormax);

        if (max == 0) {// si tout est à nul, on peut pas faire mieux, on arrette et on set le pixel
            return table;
        }
        int newblue = (colormax & 0xFF);
        int newgreen = (colormax >> 8) & 0xFF;
        int newred = (colormax >> 16) & 0xFF;
        //listecouleur = listecouleur.concat("\n(" + newred + "," + newgreen + "," + newblue + ")");
        //System.out.println(newred + "," + newgreen + "," + newblue);
        for (Integer i : couleurs) {// mise a jour de la table en fonction de la nouvelle couleur
            Customclass info = table.get(i);
            int blui = (i & 0xFF);//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int greeni = (i >> 8) & 0xFF; //same for the green component
            int redi = (i >> 16) & 0xFF;//same for the red component
            double anciennedistance = info.distance;
            double nouvelledistance = sqrt(((redi - newred) * (redi - newred) + (blui - newblue) * (blui - newblue) + (greeni - newgreen) * (greeni - newgreen)));
            if (nouvelledistance < anciennedistance) {//modification de ma custom classe à partir de ma hashtable(marche bien)
                info.distance = nouvelledistance;
                info.projectionr = newred;
                info.projectiong = newgreen;
                info.projectionb = newblue;
                //System.out.println(table.get(i).projectionb);
                //System.out.println(newblue);
            }
        }
        couleurs.remove(colormax);// ça ne sert à rien d'iterer sur les trucs dont on a deja selectionné la couleur:
        return table;
    }

    public Bitmap rotation(Bitmap bmp, double angle) {
        double ang = Math.toRadians(angle);
        int Lw = bmp.getWidth();
        int Lh = bmp.getHeight();
        int pixels[] = new int[Lw * Lh];
        bmp.getPixels(pixels, 0, Lw, 0, 0, Lw, Lh);
        int diago = (int) (Math.sqrt(Lw * Lw + Lh * Lh) + 0.5);
        int centre = diago / 2;
        int lh = (int) (Math.abs(Math.sin(ang)) * Lw + Math.abs(Math.cos(ang)) * Lh) + 1;
        int lw = (int) (Math.abs(Math.cos(ang)) * Lw + Math.abs(Math.sin(ang)) * Lh) + 1;
        int centreW = lw / 2;
        int centreH = lh / 2;
        Bitmap rota = Bitmap.createBitmap(lw, lh, Bitmap.Config.ARGB_8888);
        int pixels2[] = new int[lw * lh];
        rota.getPixels(pixels2, 0, lw, 0, 0, lw, lh);
        for (int x = 0; x < lw; x++) {
            for (int y = 0; y < lh; y++) {
                double x1 = Math.cos(ang) * (x - centreW) - Math.sin(ang) * (y - centreH) + Lw / 2;
                double y1 = Math.sin(ang) * (x - centreW) + Math.cos(ang) * (y - centreH) + Lh / 2;
                int x2 = (int) (x1 + 0.5);
                int y2 = (int) (y1 + 0.5);
                int i = y2 * Lw + x2;
                if (x2 >= 0 && x2 < Lw && y2 >= 0 && y2 < Lh) {
                    pixels2[y * lw + x] = pixels[i];
                } else {
                    pixels2[y * lw + x] = 0;
                }
            }
        }
        rota.setPixels(pixels2, 0, lw, 0, 0, lw, lh);
        return rota;
    }


    public Bitmap contourepidemique(Bitmap b, double seuil) {// cette fonction est encore un echec :( sa fait presque la meme chose qu'un filtre de sobel mais en moins bien et en plus lent
        //  il n'est pas censé etre possible d'avoir des pixels tous seuls avec sa normalement(donc c'est bugé)
        int[] bit = getbitcouleurs(b);
        int width = b.getWidth();
        int height = b.getHeight();
        int size = height * width;
        int[] res = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        boolean[] apasconsiderer = new boolean[size];
        int depart = (height - 1) * width + 1;

        // on barre les bords (pour itérer plus vite sur tout le reste de l'image par la suite)
        for (int i = 1; i < width - 1; i++) {
            apasconsiderer[i] = true;
            apasconsiderer[depart++] = true;
        }
        depart = width - 1;
        int cpt = 0;
        while (depart < size) {
            apasconsiderer[depart] = true;
            apasconsiderer[cpt] = true;
            depart += width;
            cpt += width;
        }
        LinkedList<Integer> listeafair = new LinkedList<>();
        //debut

        for (int i = 0; i < size; i++) {
            if (apasconsiderer[i] == false) {//nouveau truc connexe
                apasconsiderer[i] = true;
                listeafair.add(i);
                int cptconnexe = 0;
                HashSet<Integer> contours = new HashSet<>();
                while (!listeafair.isEmpty()) {
                    //ou on est
                    Integer pospixactu = listeafair.pop();
                    int couleuractu = bit[pospixactu];
                    int ractu = (couleuractu >> 16) & 0xFF;
                    int gactu = (couleuractu >> 8) & 0xFF;
                    int bactu = couleuractu & 0xFF;

                    //recup des voisins
                    int pixd = pospixactu + 1;
                    int pixb = pospixactu + width;
                    int pixg = pospixactu - 1;
                    int pixh = pospixactu - width;
                    double dist = 0.0;


                    //traitement individiuelle récursif du bas
                    if (apasconsiderer[pixb] == false) {
                        if (res[pixb] != 0) {// on a rencontré un pixel qui à été marqué comme contour
                            contours.add(pixb);
                        }
                        int couleurbas = bit[pixb];
                        int rb = (couleurbas >> 16) & 0xFF;
                        int gb = (couleurbas >> 8) & 0xFF;
                        int bb = couleurbas & 0xFF;
                        dist = sqrt((ractu - rb) * (ractu - rb) + (gactu - gb) * (gactu - gb) + (bactu - bb) * (bactu - bb)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixb);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            res[pixb] = 0;
                            apasconsiderer[pixb] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixb] = (int) (dist + 0.5);
                            contours.add(pixb);
                            //on ne l'appelle pas
                        }
                    }
                    //traitement recursif haut
                    if (apasconsiderer[pixh] == false) {
                        if (res[pixh] != 0) {// on a rencontré un pixel qui à été marqué comme contour
                            contours.add(pixh);
                        }
                        int couleurhaut = bit[pixh];
                        int rh = (couleurhaut >> 16) & 0xFF;
                        int gh = (couleurhaut >> 8) & 0xFF;
                        int bh = couleurhaut & 0xFF;
                        dist = sqrt((ractu - rh) * (ractu - rh) + (gactu - gh) * (gactu - gh) + (bactu - bh) * (bactu - bh)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixh);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            res[pixh] = 0;
                            apasconsiderer[pixh] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixh] = (int) (dist + 0.5);
                            contours.add(pixh);
                            //on ne l'appelle pas
                        }
                    }

                    //traitment gauche
                    if (apasconsiderer[pixg] == false) {
                        if (res[pixg] != 0) {// on a rencontré un pixel qui à été marqué comme contour
                            contours.add(pixg);
                        }
                        int couleurgauche = bit[pixg];
                        int rg = (couleurgauche >> 16) & 0xFF;
                        int gg = (couleurgauche >> 8) & 0xFF;
                        int bg = couleurgauche & 0xFF;
                        dist = sqrt((ractu - rg) * (ractu - rg) + (gactu - gg) * (gactu - gg) + (bactu - bg) * (bactu - bg)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixg);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            res[pixg] = 0;
                            apasconsiderer[pixg] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixg] = (int) (dist + 0.5);
                            contours.add(pixg);
                            //on ne l'appelle pas
                        }
                    }

                    //traitement droite
                    if (apasconsiderer[pixd] == false) {
                        if (res[pixd] != 0) {// on a rencontré un pixel qui à été marqué comme contour
                            contours.add(pixd);
                        }
                        int couleurdroit = bit[pixd];
                        int rd = (couleurdroit >> 16) & 0xFF;
                        int gd = (couleurdroit >> 8) & 0xFF;
                        int bd = couleurdroit & 0xFF;
                        dist = sqrt((ractu - rd) * (ractu - rd) + (gactu - gd) * (gactu - gd) + (bactu - bd) * (bactu - bd)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixd);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            res[pixd] = 0;
                            apasconsiderer[pixd] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixd] = (int) (dist + 0.5);
                            contours.add(pixd);
                            //on ne l'appelle pas
                        }
                    }
                }
                //la on a notre liste de contours dans cette composante connexe
                if (cptconnexe > 10) {//si c'est une compo assez grande, pour chaque contour, on le laisse à sa valeur et on le passe en mur infranchissable:
                    for (Integer contour : contours) {
                        apasconsiderer[contour] = true;
                    }
                } else {
                    for (Integer contour : contours) {// si non on le remeet a zero
                        res[contour] = 0;
                        //apasconsiderer[contour] = true;//pas sur
                    }
                }
            }
        }
        for (int i = 0; i < size; i++) {

            res[i] = ((bit[i] >>> 24) << 24) | (res[i] << 16) | (res[i] << 8) | res[i];
        }
        zoom.setPixels(res, 0, width, 0, 0, width, height);
        return zoom;
    }


    public Bitmap contourepidemique2(Bitmap b, double seuil) {//idem
        int[] bit = getbitcouleurs(b);
        int width = b.getWidth();
        int height = b.getHeight();
        int size = height * width;
        int[] res = new int[size];
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        boolean[] apasconsiderer = new boolean[size];
        int depart = (height - 1) * width + 1;

        // on barre les bords (pour itérer plus vite sur tout le reste de l'image par la suite)
        for (int i = 1; i < width - 1; i++) {
            apasconsiderer[i] = true;
            apasconsiderer[depart++] = true;
        }
        depart = width - 1;
        int cpt = 0;
        while (depart < size) {
            apasconsiderer[depart] = true;
            apasconsiderer[cpt] = true;
            depart += width;
            cpt += width;
        }
        LinkedList<Integer> listeafair = new LinkedList<>();
        //debut

        for (int i = 0; i < size; i++) {
            if (apasconsiderer[i] == false) {//nouveau truc connexe
                apasconsiderer[i] = true;
                listeafair.add(i);
                int cptconnexe = 0;
                HashSet<Integer> contours = new HashSet<>();
                while (!listeafair.isEmpty()) {
                    //ou on est
                    Integer pospixactu = listeafair.pop();
                    int couleuractu = bit[pospixactu];
                    int ractu = (couleuractu >> 16) & 0xFF;
                    int gactu = (couleuractu >> 8) & 0xFF;
                    int bactu = couleuractu & 0xFF;

                    //recup des voisins
                    int pixd = pospixactu + 1;
                    int pixb = pospixactu + width;
                    int pixg = pospixactu - 1;
                    int pixh = pospixactu - width;
                    double dist = 0.0;


                    //traitement individiuelle récursif du bas
                    if (apasconsiderer[pixb] == false) {
                        int couleurbas = bit[pixb];
                        int rb = (couleurbas >> 16) & 0xFF;
                        int gb = (couleurbas >> 8) & 0xFF;
                        int bb = couleurbas & 0xFF;
                        dist = sqrt((ractu - rb) * (ractu - rb) + (gactu - gb) * (gactu - gb) + (bactu - bb) * (bactu - bb)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixb);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            apasconsiderer[pixb] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixb] = (int) (dist + 0.5);
                            contours.add(pixb);

                            //on ne l'appelle pas
                        }
                    }
                    //traitement recursif haut
                    if (apasconsiderer[pixh] == false) {
                        int couleurhaut = bit[pixh];
                        int rh = (couleurhaut >> 16) & 0xFF;
                        int gh = (couleurhaut >> 8) & 0xFF;
                        int bh = couleurhaut & 0xFF;
                        dist = sqrt((ractu - rh) * (ractu - rh) + (gactu - gh) * (gactu - gh) + (bactu - bh) * (bactu - bh)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixh);// on l'appelle. On s'arrange pour ne plus le rappeler plus tard
                            apasconsiderer[pixh] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixh] = (int) (dist + 0.5);
                            contours.add(pixh);
                            //on ne l'appelle pas
                        }
                    }

                    //traitment gauche
                    if (apasconsiderer[pixg] == false) {
                        int couleurgauche = bit[pixg];
                        int rg = (couleurgauche >> 16) & 0xFF;
                        int gg = (couleurgauche >> 8) & 0xFF;
                        int bg = couleurgauche & 0xFF;
                        dist = sqrt((ractu - rg) * (ractu - rg) + (gactu - gg) * (gactu - gg) + (bactu - bg) * (bactu - bg)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixg);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            apasconsiderer[pixg] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixg] = (int) (dist + 0.5);
                            contours.add(pixg);
                            //on ne l'appelle pas
                        }
                    }

                    //traitement droite
                    if (apasconsiderer[pixd] == false) {
                        int couleurdroit = bit[pixd];
                        int rd = (couleurdroit >> 16) & 0xFF;
                        int gd = (couleurdroit >> 8) & 0xFF;
                        int bd = couleurdroit & 0xFF;
                        dist = sqrt((ractu - rd) * (ractu - rd) + (gactu - gd) * (gactu - gd) + (bactu - bd) * (bactu - bd)) - seuil;
                        if (dist <= 0) {
                            cptconnexe++;
                            listeafair.add(pixd);// on l'appelle, et si avant on avait considérer sa comme un contour, on le le change. On s'arrange pour ne plus le rappeler plus tard
                            apasconsiderer[pixd] = true;
                        } else {//si ça passe pas, c'est que c'est actuellement un contour
                            res[pixd] = (int) (dist + 0.5);
                            contours.add(pixd);
                            //on ne l'appelle pas
                        }
                    }
                }
                //la on a notre liste de contours dans cette composante connexe
                if (cptconnexe > 20) {//si c'est une compo assez grande, pour chaque contour, on le laisse à sa valeur et on le passe en mur infranchissable:
                    for (Integer contour : contours) {
                        apasconsiderer[contour] = true;
                    }

                } else {
                    for (Integer contour : contours) {
                        res[contour] = 0;
                        //pas sur
                    }
                }
            }
        }
        for (int i = 0; i < size; i++) {

            res[i] = ((bit[i] >>> 24) << 24) | (res[i] << 16) | (res[i] << 8) | res[i];
        }
        zoom.setPixels(res, 0, width, 0, 0, width, height);
        return zoom;
    }


//bon, ben j'essayerai un dernier truc avant de fair un filtre de canny pour les contours, sa me saoul!


    public Bitmap grain(Bitmap b, int intensite) {// bon les appelles de random sont super longs. La question est donc comment fais un truc aléatoire sans random
        Random gen = new Random();
        int w = b.getWidth();
        int h = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = h * w;
        int[] tab = new int[size];
        b.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixels
        for (int i = 0; i < size; i++) {
            int tmp = tab[i];
            int a = tmp >>> 24;
            int blue = tmp & 0xFF;//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int green = (tmp >> 8) & 0xFF;//same for the green component
            int red = (tmp >> 16) & 0xFF;//same for the red component
            if (gen.nextInt(2) == 0) {
                blue -= gen.nextInt(intensite);
            } else {
                blue += gen.nextInt(intensite);
            }
            if (gen.nextInt(2) == 0) {
                green -= gen.nextInt(intensite);
            } else {
                green += gen.nextInt(intensite);
            }
            if (gen.nextInt(2) == 0) {
                red -= gen.nextInt(intensite);
            } else {
                red += gen.nextInt(intensite);
            }
            if (red > 255) {
                red = 255;
            }
            if (green > 255) {
                green = 255;
            }
            if (blue > 255) {
                blue = 255;
            }

            if (red < 0) {
                red = 0;
            }
            if (blue < 0) {
                blue = 0;
            }
            if (green < 0) {
                green = 0;
            }

            int final_pix = (a << 24) | (red << 16) | (green << 8) | blue;//Makes an integer matching the Color's formatting
            tab[i] = final_pix;
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }

    public Bitmap grain2(Bitmap b, int intensite) {// bon les appelles de random sont super longs. La question est donc comment fais un truc aléatoire sans random
        int w = b.getWidth();
        int h = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = h * w;
        int[] tab = new int[size];
        b.getPixels(tab, 0, w, 0, 0, w, h);//Gets the array of the bitmap's pixels
        int seed = 0;
        int seed2 = 0;
        int seed3 = 1;
        for (int i = 0; i < size; i++) {
            if (seed > 10) {
                seed = 0;
            }
            seed++;
            int tmp = tab[i];
            int a = tmp >>> 24;
            int blue = tmp & 0xFF;//Gets the blue component of the pixel by filtering the integer Color and weight the average
            int green = (tmp >> 8) & 0xFF;//same for the green component
            int red = (tmp >> 16) & 0xFF;//same for the red component
//la on chope un entier < size en faisant n'importe quoi; on est obligé de faire un truc pour travailler sur un pixel éloigné au hasard, sinon y'a
// (dans de rare cas) des effets locaux étrange si l'image comporte de large zone à 000
            // la du coup c'est mieux, mais y'a encore des trucs bizarre
            int pixpseudoaleatoire = seed * (blue + green - red) + seed2 + seed3;
            if (pixpseudoaleatoire < 0) {
                pixpseudoaleatoire = -pixpseudoaleatoire;
            }
            pixpseudoaleatoire = pixpseudoaleatoire % size;
            int randomcolor = tab[pixpseudoaleatoire];
            int randb = randomcolor & 0xFF;
            int randr = (randomcolor >> 16) & 0xFF;
            int pseudoaleatoire = seed * (randb + green - randr) + seed2 + seed3;
            int valeur = pseudoaleatoire % intensite;
            //ajouté ou soustraire

            if ((pseudoaleatoire & 0x1) == 0) {
                blue -= valeur;
            } else {
                blue += valeur;
            }

            if ((seed2 & 0x1) == 0) {
                green -= valeur;
            } else {
                green += valeur;
            }

            if ((seed3 & 0x1) == 0) {
                red -= valeur;
            } else {
                red += valeur;
            }


            seed2 = seed3;
            seed3 = pixpseudoaleatoire;

            if (red > 255) {
                red = 255;
            }
            if (green > 255) {
                green = 255;
            }
            if (blue > 255) {
                blue = 255;
            }

            if (red < 0) {
                red = 0;
            }
            if (blue < 0) {
                blue = 0;
            }
            if (green < 0) {
                green = 0;
            }

            int final_pix = (a << 24) | (red << 16) | (green << 8) | blue;//Makes an integer matching the Color's formatting
            tab[i] = final_pix;
        }
        zoom.setPixels(tab, 0, w, 0, 0, w, h);
        return zoom;
    }

    public Bitmap vieeux(Bitmap b, double intensite) {
        return teintrapidelegere(flou(grain2(toGray2(b), 30), 3), 112, 66, 20, intensite);
        // return teintrapidelegere(b, 112,66,20,intensite);
    }

    public Bitmap flou(Bitmap b, int intensite) {//ça, c'étais pour apprendre à iterer en cercle, en vrai faut faire avec les carrés.
        int width = b.getWidth();
        int height = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] tab = new int[size];
        double[] tabledescordes = new double[intensite + 1];
        int[] res = new int[size];
        for (int i = 0; i < intensite + 1; i++) {
            tabledescordes[i] = sqrt(intensite * intensite - (i * i));
        }
        for (int i = 0; i < intensite + 1; i++) {
            //System.out.println(i+ " " +tabledescordes[i]);
        }

        b.getPixels(tab, 0, width, 0, 0, width, height);//Gets the array of the bitmap's pixels
        int x = 0;
        int y = 0;
        int haut = 0;
        int bas = intensite;
        for (int i = 0; i < size; i++, x++) {

            if (x >= width) {
                x = 0;
                if (y > intensite) {
                    haut++;
                }
                y++;
                bas++;
            }
            //System.out.println(x +" "+ y);

            int tmp = tab[i];
            int a = tmp >>> 24;
            int sommer = (tmp >> 16) & 0xFF;
            int sommeg = (tmp >> 8) & 0xFF;
            int sommeb = tmp & 0xFF;
            ;
            int cpt = 1;


            if (bas >= height) {//= ???
                bas = height - 1;//-1?
            }

            //System.out.println(bas +" "+ haut + " " + y);
            //System.out.println("autre pixel");
            int cpty = 1;

            for (int ity = bas - y; ity > haut - y; ity--) {// on va itérer sur un cercle autour de la zone actuelle.(pour cela, on iterere sur la verticalité du cerle, puis
                //grace à pythagore on calcule la corde à cette endroit du cercle;
                //ystem.out.println(ity);
                int absity = ity;
                if (ity < 0) {
                    absity = -ity;
                }
                int corde = (int) tabledescordes[absity];

                // la on a la longueur sur laquelle itérer, faut faire attention à pas dépasser;
                int gauche = x - corde;
                if (gauche < 0) {
                    gauche = 0;
                }
                int droite = x + corde;
                if (droite >= width) {//>?
                    droite = width - 1;//+1 -1??
                }

                //System.out.println(droite);
                for (int itx = gauche; itx < droite; itx++) {
                    int colorit = tab[itx + width * (ity + y)];// à améliorer pour pas calculer width puis 2 width puis 3 width, mais juste +=width.
                    int rit = (colorit >> 16) & 0xFF;
                    int git = (colorit >> 8) & 0xFF;
                    int bit = colorit & 0xFF;

                    sommeb += bit;
                    sommeg += git;
                    sommer += rit;
                    cpt++;
                }
            }
            int newr = (int) (((double) sommer / cpt) + 0.5);
            int newb = (int) (((double) sommeb / cpt) + 0.5);
            int newg = (int) (((double) sommeg / cpt) + 0.5);
            res[i] = (a << 24) | (newr << 16) | (newg << 8) | newb;
        }
        zoom.setPixels(res, 0, width, 0, 0, width, height);
        return zoom;
    }

    public Bitmap flourapide(Bitmap b, int intensite) {//on va essayer de pas repasser 800 fois sur le meme pixel.
        int width = b.getWidth();
        int height = b.getHeight();
        Bitmap zoom = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        zoom.setDensity(b.getDensity());
        int size = height * width;
        int[] tab = new int[size];
        double[] tabledescordes = new double[intensite + 1];
        int[] res = new int[size];
        int x = 0;
        int y = 0;
        int haut = 0;
        int bas = intensite;
        for (int i = 0; i < size; i++) {
            if (x >= width) {
                x = 0;
                if (y > intensite) {
                    haut++;
                }
                y++;
                bas++;
            }

            if (bas >= height) {//= ???
                bas = height - 1;//-1?
            }

            int gauche;

        }
        return b;
    }
}

