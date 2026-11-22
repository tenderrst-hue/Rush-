package com.tenderrst.rushprank;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {
    private static final int REQ_FILES = 410;
    private static final int REQ_OVERLAY = 411;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private GameView game;
    private WindowManager overlayManager;
    private View flashView;
    private long deadline;
    private int phase = 0; // 0 permissions, 1 flash, 2 search, 3 cabinet, 4 hidden, 5 rush, 6 caught
    private boolean sequenceStarted = false;
    private String imagePath = "";
    private String folderPath = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        game = new GameView();
        setContentView(game);

        long savedDeadline = getPreferences(0).getLong("deadline", 0);
        boolean wasHidden = getPreferences(0).getBoolean("hidden", false);
        if (savedDeadline > 0 && System.currentTimeMillis() < savedDeadline) {
            deadline = savedDeadline;
            phase = wasHidden ? 4 : 2;
            tick();
        } else if (savedDeadline > 0 && System.currentTimeMillis() >= savedDeadline) {
            phase = wasHidden ? 5 : 6;
            game.invalidate();
            if (phase == 6) handler.postDelayed(this::finish, 3000);
        } else if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            deadline = savedDeadline;
            imagePath = getPreferences(0).getString("imagePath", "");
            folderPath = getPreferences(0).getString("folderPath", "");
            boolean hidden = getPreferences(0).getBoolean("hidden", false);
            if (deadline > 0 && System.currentTimeMillis() >= deadline) phase = hidden ? 5 : 6;
            else phase = hidden ? 4 : 3;
            if (deadline > 0 && System.currentTimeMillis() < deadline) tick();
            else if (phase == 6) handler.postDelayed(this::finish, 3500);
        } else {
            phase = 0;
            showPermissionScreen();
        }
    }

    private void showPermissionScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(Color.rgb(10, 9, 12));
        TextView title = new TextView(this);
        title.setText("RUSH PRANK");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView info = new TextView(this);
        info.setText("Для розыгрыша нужны два разрешения: показ короткой вспышки поверх рабочего стола и создание одного PNG в выбранной папке Documents. Приложение не читает и не удаляет ваши файлы.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, 24, 0, 24);
        root.addView(info, ip);
        Button go = new Button(this);
        go.setText("РАЗРЕШИТЬ И НАЧАТЬ");
        root.addView(go);
        go.setOnClickListener(v -> requestNeededPermissions());
        setContentView(root);
    }

    private void requestNeededPermissions() {
        if (!Environment.isExternalStorageManager()) {
            try {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(i, REQ_FILES);
            } catch (Exception e) {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION), REQ_FILES);
            }
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
            return;
        }
        startSequence();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILES || requestCode == REQ_OVERLAY) {
            if (Environment.isExternalStorageManager() && Settings.canDrawOverlays(this)) startSequence();
            else {
                Toast.makeText(this, "Нужны оба разрешения, чтобы запустить выбранный сценарий", Toast.LENGTH_LONG).show();
                showPermissionScreen();
            }
        }
    }

    private void startSequence() {
        if (sequenceStarted) return;
        sequenceStarted = true;
        setContentView(game);
        try {
            createCabinetPng();
            startDesktopFlash();
        } catch (Exception e) {
            sequenceStarted = false;
            Toast.makeText(this, "Не удалось создать PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void createCabinetPng() throws Exception {
        File docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String a = "System_" + (100 + new Random().nextInt(900));
        String b = "Cache_" + (100 + new Random().nextInt(900));
        File folder = new File(new File(docs, a), b);
        if (!folder.mkdirs() && !folder.isDirectory()) throw new Exception("не удалось создать папку Documents");
        File png = new File(folder, "closet_" + (10 + new Random().nextInt(89)) + ".png");
        Bitmap bmp = Bitmap.createBitmap(512, 700, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        drawCabinet(c, 512, 700);
        FileOutputStream out = new FileOutputStream(png);
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
        bmp.recycle();
        folderPath = "Documents/" + a + "/" + b + "/";
        imagePath = png.getAbsolutePath();
        getPreferences(0).edit().putString("imagePath", imagePath).putString("folderPath", folderPath).apply();
    }

    private void startDesktopFlash() {
        phase = 1;
        final android.widget.FrameLayout flash = new android.widget.FrameLayout(this);
        flash.setBackgroundColor(Color.WHITE);
        flashView = flash;
        overlayManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.OPAQUE);
        lp.gravity = Gravity.TOP | Gravity.START;
        overlayManager.addView(flashView, lp);
        handler.postDelayed(() -> moveTaskToBack(true), 250);
        final int[] step = {0};
        Runnable pulse = new Runnable() {
            @Override public void run() {
                if (flashView == null) return;
                step[0]++;
                flash.setBackgroundColor((step[0] % 2 == 0) ? Color.WHITE : Color.BLACK);
                if (step[0] < 10) handler.postDelayed(this, 300);
                else {
                    try { overlayManager.removeView(flashView); } catch (Exception ignored) {}
                    flashView = null;
                    deadline = System.currentTimeMillis() + 30000L;
                    getPreferences(0).edit().putLong("deadline", deadline).putBoolean("hidden", false).apply();
                    phase = 2;
                    postTimerNotification();
                    tick();
                }
            }
        };
        handler.postDelayed(pulse, 300);
    }

    private void postTimerNotification() {
        // The countdown is persisted so it survives the app being opened from the file manager.
    }

    private void tick() {
        if (deadline <= 0) return;
        long left = deadline - System.currentTimeMillis();
        if (left <= 0) {
            boolean hidden = getPreferences(0).getBoolean("hidden", false);
            phase = hidden ? 5 : 6;
            game.invalidate();
            if (!hidden) handler.postDelayed(this::finish, 3500);
            return;
        }
        game.secondsLeft = (int)Math.ceil(left / 1000.0);
        if (game != null) game.invalidate();
        handler.postDelayed(this::tick, 200);
    }

    private void enterCabinet() {
        if (phase == 2 || phase == 3) {
            phase = 4;
            getPreferences(0).edit().putBoolean("hidden", true).apply();
            game.invalidate();
        }
    }

    private void finish() { finishAndRemoveTask(); }

    private static void drawCabinet(Canvas c, int w, int h) {
        Paint p = new Paint(3);
        c.drawColor(Color.rgb(25, 13, 8));
        p.setColor(Color.rgb(62,31,17)); c.drawRoundRect(25,25,w-25,h-20,18,18,p);
        p.setColor(Color.rgb(125,70,37)); c.drawRect(45,58,w-45,h-38,p);
        p.setColor(Color.rgb(83,43,23)); c.drawRect(w/2-9,65,w/2+9,h-42,p);
        p.setColor(Color.rgb(157,94,52)); c.drawRoundRect(62,82,w/2-18,h-55,8,8,p);
        c.drawRoundRect(w/2+18,82,w-62,h-55,8,8,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(10); p.setColor(Color.rgb(78,39,22));
        c.drawRect(82,110,w/2-38,h/2-12,p); c.drawRect(82,h/2+15,w/2-38,h-82,p);
        c.drawRect(w/2+38,110,w-82,h/2-12,p); c.drawRect(w/2+38,h/2+15,w-82,h-82,p);
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(239,211,151));
        c.drawCircle(w/2-34,h/2,12,p); c.drawCircle(w/2+34,h/2,12,p);
    }

    private class GameView extends View {
        Paint p = new Paint(3);
        int secondsLeft = 30;
        GameView() { super(MainActivity.this); }
        void text(Canvas c,String s,float x,float y,float size,int color) {
            p.setColor(color); p.setTextSize(size); p.setTypeface(android.graphics.Typeface.create("sans-serif-condensed",1)); p.setTextAlign(Paint.Align.CENTER); c.drawText(s,x,y,p);
        }
        void button(Canvas c,String s,float x,float y,float w,float h) {
            p.setColor(Color.rgb(110,48,25)); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,18,18,p);
            p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawRoundRect(x-w/2,y-h/2,x+w/2,y+h/2,18,18,p); p.setStyle(Paint.Style.FILL);
            text(c,s,x,y+9,21,Color.WHITE);
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            c.drawColor(Color.rgb(7,6,8));
            if (phase == 0) {
                text(c,"RUSH PRANK",w/2,h*.42f,36,Color.WHITE);
                text(c,"Нажми, чтобы выдать разрешения",w/2,h*.51f,18,Color.LTGRAY);
            } else if (phase == 2) {
                text(c,"НАЙДИ ИЗОБРАЖЕНИЕ ШКАФА",w/2,h*.17f,25,Color.WHITE);
                text(c,"Открой файловый менеджер телефона",w/2,h*.25f,18,Color.LTGRAY);
                text(c,folderPath,w/2,h*.34f,16,Color.rgb(231,176,115));
                text(c,"Файл: " + new File(imagePath).getName(),w/2,h*.39f,16,Color.LTGRAY);
                text(c,String.format(Locale.US,"%02d",secondsLeft),w/2,h*.57f,70,Color.WHITE);
                text(c,"СЕКУНД ДО RUSH",w/2,h*.63f,17,Color.LTGRAY);
                text(c,"Найди PNG и открой его через Rush Prank",w/2,h*.76f,16,Color.LTGRAY);
            } else if (phase == 3 || phase == 4) {
                text(c,phase==4 ? "ТИШЕ. НЕ ДВИГАЙСЯ." : "ШКАФ НАЙДЕН",w/2,h*.13f,25,Color.WHITE);
                if (phase==3) {
                    drawCabinetScaled(c,w*.12f,h*.23f,w*.76f,h*.58f);
                    button(c,"ЗАЛЕЗТЬ В ШКАФ",w/2,h*.89f,w*.78f,62);
                } else {
                    p.setColor(Color.rgb(82,43,24)); c.drawRect(w*.10f,h*.18f,w*.90f,h*.86f,p);
                    p.setColor(Color.rgb(8,7,8)); c.drawRect(w*.485f,h*.18f,w*.515f,h*.86f,p);
                    p.setColor(Color.rgb(187,135,80)); c.drawRect(w*.49f,h*.48f,w*.51f,h*.54f,p);
                    text(c,"СМОТРИ В ЩЕЛЬ",w/2,h*.93f,19,Color.LTGRAY);
                }
                text(c,String.format(Locale.US,"%02d",secondsLeft),w/2,h*.08f,23,Color.WHITE);
            } else if (phase == 5) {
                text(c,"RUSH ПРОЛЕТАЕТ ЗА ДВЕРЦАМИ",w/2,h*.12f,21,Color.WHITE);
                p.setColor(Color.rgb(83,43,24)); c.drawRect(w*.10f,h*.20f,w*.90f,h*.86f,p);
                p.setColor(Color.BLACK); c.drawRect(w*.485f,h*.20f,w*.515f,h*.86f,p);
                p.setColor(Color.rgb(150,151,178)); c.drawOval(w*.44f,h*.40f,w*.56f,h*.60f,p);
                text(c,"ТЫ СПРЯТАЛСЯ!",w/2,h*.93f,26,Color.WHITE);
            } else if (phase == 6) {
                drawRush(c,w,h);
                text(c,"ТЕБЯ НАШЁЛ RUSH",w/2,h*.90f,27,Color.WHITE);
            }
        }
        private void drawCabinetScaled(Canvas c,float x,float y,float width,float height) {
            c.save(); c.translate(x,y); c.scale(width/512f,height/700f); drawCabinet(c,512,700); c.restore();
        }
        private void drawRush(Canvas c,float w,float h) {
            p.setColor(Color.rgb(12,11,17)); c.drawCircle(w/2,h*.48f,Math.min(w,h)*.37f,p);
            p.setColor(Color.rgb(133,137,169)); c.drawOval(w*.29f,h*.35f,w*.45f,h*.47f,p); c.drawOval(w*.55f,h*.35f,w*.71f,h*.47f,p);
            p.setColor(Color.BLACK); c.drawOval(w*.32f,h*.36f,w*.42f,h*.46f,p); c.drawOval(w*.58f,h*.36f,w*.68f,h*.46f,p);
            p.setColor(Color.WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8); c.drawArc(w*.32f,h*.43f,w*.68f,h*.67f,5,170,false,p); p.setStyle(Paint.Style.FILL);
            for(int i=0;i<22;i++){ double a=i*Math.PI*2/22; c.drawLine(w/2+(float)Math.cos(a)*Math.min(w,h)*.34f,h*.48f+(float)Math.sin(a)*Math.min(w,h)*.34f,w/2+(float)Math.cos(a)*Math.min(w,h)*.43f,h*.48f+(float)Math.sin(a)*Math.min(w,h)*.43f,p); }
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()==MotionEvent.ACTION_UP && phase==3) {
                enterCabinet(); invalidate(); return true;
            }
            return true;
        }
    }
}