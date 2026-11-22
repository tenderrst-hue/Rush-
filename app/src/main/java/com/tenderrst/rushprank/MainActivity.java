package com.tenderrst.rushprank;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameView game;
    private int phase = 0; // 0 flash, 1 search, 2 cabinet, 3 hidden, 4 rush, 5 caught
    private long deadline;
    private long rushStart;
    private int flashStep = 0;
    private String folderName = "";
    private String savedLocation = "";
    private boolean finished = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        game = new GameView();
        setContentView(game);
        startFlash();
    }

    private void startFlash() {
        phase = 0;
        final Runnable flash = new Runnable() {
            @Override public void run() {
                if (phase != 0) return;
                flashStep++;
                game.invalidate();
                if (flashStep < 17) handler.postDelayed(this, 180);
                else beginSearch();
            }
        };
        handler.postDelayed(flash, 180);
    }

    private void beginSearch() {
        phase = 1;
        folderName = "Room_" + (100 + new Random().nextInt(900));
        saveCabinetPng();
        deadline = System.currentTimeMillis() + 30000L;
        tick();
    }

    private void saveCabinetPng() {
        Bitmap bmp = Bitmap.createBitmap(512, 700, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        drawCabinet(c, 512, 700);
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, "closet_" + folderName + ".png");
            values.put(MediaStore.Downloads.MIME_TYPE, "image/png");
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/RushPrank/" + folderName + "/");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                if (out != null) { bmp.compress(Bitmap.CompressFormat.PNG, 100, out); out.close(); }
                savedLocation = "Download/RushPrank/" + folderName + "/closet_" + folderName + ".png";
            }
        } catch (Exception ignored) { savedLocation = "Не удалось сохранить PNG"; }
        bmp.recycle();
    }

    private void tick() {
        if ((phase != 1 && phase != 2 && phase != 3) || finished) return;
        long left = Math.max(0, deadline - System.currentTimeMillis());
        game.secondsLeft = (int)Math.ceil(left / 1000.0);
        game.invalidate();
        if (left <= 0) {
            boolean wasHidden = phase == 3;
            phase = wasHidden ? 6 : 4;
            rushStart = System.currentTimeMillis();
            handler.postDelayed(() -> {
                if (phase == 6) { phase = 7; game.invalidate(); }
                else if (phase == 4) {
                    phase = 5;
                    game.invalidate();
                    handler.postDelayed(() -> { if (phase == 5 && !finished) { finished = true; finish(); } }, 2600);
                }
            }, 2100);
        } else handler.postDelayed(this::tick, 100);
    }

    private void enterCabinet() { if (phase == 1) { phase = 2; game.invalidate(); } }
    private void hideInCabinet() { if (phase == 2) { phase = 3; game.invalidate(); } }

    @Override public void onBackPressed() {
        if (phase == 3 || phase == 4 || phase == 5) { super.onBackPressed(); }
        else { super.onBackPressed(); }
    }

    private static void drawCabinet(Canvas c, int w, int h) {
        Paint p = new Paint(3);
        c.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
        p.setColor(Color.rgb(55,27,14)); c.drawRoundRect(30,35,w-30,h-25,18,18,p);
        p.setColor(Color.rgb(120,66,34)); c.drawRect(48,65,w-48,h-40,p);
        p.setColor(Color.rgb(83,43,23)); c.drawRect(w/2-8,75,w/2+8,h-45,p);
        p.setColor(Color.rgb(153,91,48)); c.drawRoundRect(65,95,w/2-18,h-65,8,8,p);
        c.drawRoundRect(w/2+18,95,w-65,h-65,8,8,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(9); p.setColor(Color.rgb(78,39,22));
        c.drawRect(85,120,w/2-38,h/2-12,p); c.drawRect(85,h/2+18,w/2-38,h-95,p);
        c.drawRect(w/2+38,120,w-85,h/2-12,p); c.drawRect(w/2+38,h/2+18,w-85,h-95,p);
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(238,211,151));
        c.drawCircle(w/2-34,h/2,11,p); c.drawCircle(w/2+34,h/2,11,p);
    }

    private class GameView extends View {
        Paint p = new Paint(3);
        int secondsLeft = 30;
        GameView() { super(MainActivity.this); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        void text(Canvas c, String s, float x, float y, float size, int color) {
            p.setColor(color); p.setTextSize(size); p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed", 1)); p.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,p);
        }
        void button(Canvas c, String s, float x, float y, float w, float h) {
            p.setColor(Color.rgb(110,48,25)); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,18,18,p);
            p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,18,18,p); p.setStyle(Paint.Style.FILL);
            text(c,s,x,y+10,23,Color.WHITE);
        }
        @Override protected void onDraw(Canvas raw) {
            super.onDraw(raw);
            Canvas c = raw; float w=getWidth(), h=getHeight();
            c.drawColor(Color.rgb(8,7,9));
            if (phase == 0) {
                c.drawColor((flashStep % 2 == 0) ? Color.WHITE : Color.BLACK);
                return;
            }
            if (phase == 1) {
                text(c,"НАЙДИ ШКАФ",w/2,h*.16f,34,Color.WHITE);
                text(c,"Открой приложение «Файлы»",w/2,h*.23f,21,Color.LTGRAY);
                text(c,"Download / RushPrank / "+folderName,w/2,h*.29f,16,Color.rgb(230,174,111));
                text(c,"Открой PNG шкафа, затем вернись сюда",w/2,h*.34f,17,Color.LTGRAY);
                p.setColor(Color.rgb(130,70,37)); c.drawRoundRect(w*.18f,h*.40f,w*.82f,h*.70f,16,16,p);
                drawCabinet(c,(int)(w*.64f),(int)(h*.30f));
                text(c,String.format(Locale.US,"%02d",secondsLeft),w/2,h*.79f,62,Color.WHITE);
                text(c,"СЕКУНД",w/2,h*.83f,17,Color.LTGRAY);
                button(c,"Я НАШЁЛ ШКАФ",w/2,h*.91f,w*.72f,62);
            } else if (phase == 2) {
                text(c,"ТЫ ВНУТРИ ШКАФА?",w/2,h*.18f,28,Color.WHITE);
                drawCabinet(c,(int)(w*.86f),(int)(h*.58f));
                button(c,"СПРЯТАТЬСЯ",w/2,h*.88f,w*.72f,64);
            } else if (phase == 3) {
                text(c,"ТИШЕ...",w/2,h*.14f,32,Color.WHITE);
                p.setColor(Color.rgb(83,43,23)); c.drawRect(w*.12f,h*.24f,w*.88f,h*.83f,p);
                p.setColor(Color.rgb(12,9,10)); c.drawRect(w*.48f,h*.24f,w*.52f,h*.83f,p);
                p.setColor(Color.rgb(190,137,82)); c.drawRect(w*.49f,h*.46f,w*.51f,h*.53f,p);
                text(c,"СМОТРИ В ЩЕЛЬ",w/2,h*.91f,20,Color.LTGRAY);
            } else if (phase == 4) {
                float t=Math.min(1f,(System.currentTimeMillis()-rushStart)/1900f);
                drawRush(c,w,h,t);
                postInvalidateDelayed(35);
            } else if (phase == 6 || phase == 7) {
                p.setColor(Color.rgb(83,43,23)); c.drawRect(w*.12f,h*.18f,w*.88f,h*.88f,p);
                p.setColor(Color.rgb(10,8,9)); c.drawRect(w*.40f,h*.18f,w*.60f,h*.88f,p);
                p.setColor(Color.rgb(185,132,78)); c.drawRect(w*.485f,h*.48f,w*.515f,h*.55f,p);
                if (phase == 6) {
                    c.save();
                    c.clipRect(w*.405f,h*.20f,w*.595f,h*.86f);
                    float t=Math.min(1f,(System.currentTimeMillis()-rushStart)/1900f);
                    drawRush(c,w,h,t);
                    c.restore();
                    postInvalidateDelayed(35);
                } else text(c,"RUSH ПРОЛЕТЕЛ!",w/2,h*.94f,22,Color.WHITE);
                text(c,phase==6 ? "НЕ ШЕВЕЛИСЬ..." : "ТЫ СПРЯТАЛСЯ",w/2,h*.12f,23,Color.WHITE);
            } else if (phase == 5) {
                drawRush(c,w,h,1f);
                text(c,"ТЕБЯ НАШЁЛ RUSH",w/2,h*.88f,28,Color.WHITE);
            }
        }
        void drawRush(Canvas c,float w,float h,float t) {
            c.drawColor(Color.BLACK);
            float cx=w/2, cy=h*(.48f + (1-t)*.8f), r=Math.min(w,h)*(.18f+t*.43f);
            p.setColor(Color.rgb(15,14,20)); c.drawCircle(cx,cy,r,p);
            p.setColor(Color.rgb(48,47,60)); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8);
            for(int i=0;i<70;i++){ double a=i*Math.PI*2/70; c.drawLine(cx+(float)Math.cos(a)*r*.85f,cy+(float)Math.sin(a)*r*.85f,cx+(float)Math.cos(a)*r*1.12f,cy+(float)Math.sin(a)*r*1.12f,p); }
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(150,151,178));
            c.drawOval(cx-r*.52f,cy-r*.32f,cx-r*.08f,cy+r*.05f,p); c.drawOval(cx+r*.08f,cy-r*.32f,cx+r*.52f,cy+r*.05f,p);
            p.setColor(Color.BLACK); c.drawOval(cx-r*.40f,cy-r*.28f,cx-r*.20f,cy, p); c.drawOval(cx+r*.20f,cy-r*.28f,cx+r*.40f,cy,p);
            p.setColor(Color.WHITE); c.drawArc(cx-r*.48f,cy-r*.02f,cx+r*.48f,cy+r*.48f,5,170,false,p);
            p.setStrokeWidth(7); p.setStyle(Paint.Style.STROKE); c.drawArc(cx-r*.48f,cy-r*.02f,cx+r*.48f,cy+r*.48f,5,170,false,p); p.setStyle(Paint.Style.FILL);
            if (phase==4) postInvalidateDelayed(35);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()!=MotionEvent.ACTION_UP) return true;
            float y=e.getY()/getHeight();
            if (phase==1 && y>.84f) enterCabinet();
            else if (phase==2 && y>.76f) hideInCabinet();
            invalidate(); return true;
        }
    }
}
