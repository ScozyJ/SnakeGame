package com.codelab.jeremy.snakegame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

/**
 * Created by Jeremy on 10/7/2017.
 */

public class GameActivity extends Activity {
    Canvas canvas;
    SnakeView snakeView;
    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    //Initialize sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //for snake movement 0 = up, 1 = right, 2 = down, 3 = left
    int directionOfTravel = 0;

    int screenWidth;
    int screenHeight;
    int topGap;

    //stats
    long lastFrameTime;
    int fps;
    int score;
    int high;

    //Game Objects
    int[] snakeX;
    int[] snakeY;
    int snakeLength;
    int appleX;
    int appleY;

    //the size in pixels of a place on the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadSound();
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        while (true) {
            snakeView.pause();
            break;
        }
        finish();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            snakeView.pause();
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void loadSound() {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            sample1 = soundPool.load(this, R.raw.sample1, 0);
            sample2 = soundPool.load(this, R.raw.sample2, 0);
            sample3 = soundPool.load(this, R.raw.sample3, 0);
            sample4 = soundPool.load(this, R.raw.sample4, 0);

    }

    public void configureDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight / 14;
        blockSize = screenWidth / 40;
        numBlocksWide = 40;
        numBlocksHigh = ((screenHeight - topGap)) / blockSize;

        headBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.head);
        bodyBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body);
        tailBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tail);
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);

        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);
    }

    class SnakeView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            //snake length
            snakeX = new int[200];
            snakeY = new int[200];

            //our starting snake
            getSnake();

            //get an apple to much
            getApple();
        }

        public void getSnake() {
            snakeLength = 3;
            //snake head starts in the middle of screen
            snakeX[0] = numBlocksWide / 2;
            snakeY[0] = numBlocksHigh / 2;

            //then the body
            snakeX[1] = snakeX[0] - 1;
            snakeY[1] = snakeY[0];

            //finally the tail
            snakeX[1] = snakeX[1] - 1;
            snakeY[1] = snakeY[0];
        }

        public void getApple() {
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide - 1) + 1;
            appleY = random.nextInt(numBlocksHigh - 1) + 1;
        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame() {
            //did the player get the apple
            if (snakeX[0] == appleX && snakeY[0] == appleY) {
                //grow the snake
                snakeLength++;
                //replace the apple
                getApple();
                //add to the score
                score = score + snakeLength;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }
            //move the snake starting at the back
            for (int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];
            }
            //move the head in the appropriate direction
            switch (directionOfTravel) {
                case 0://up
                    snakeY[0]--;
                    break;

                case 1://right
                    snakeX[0]++;
                    break;

                case 2://down
                    snakeY[0]++;
                    break;

                case 3://left
                    snakeX[0]--;
                    break;
            }

            //have we had an accident?
            boolean dead = false;
            //-> accident with a wall
            if (snakeX[0] == -1) dead = true;
            if (snakeX[0] >= numBlocksWide) dead = true;
            if (snakeY[0] == -1) dead = true;
            if (snakeY[0] == numBlocksHigh) dead = true;

            //-> accident have we eaten ourselves
            for (int i = snakeLength - 1; i > 0; i--) {
                if ((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }

            if (dead) {
                //start again
                soundPool.play(sample4, 1, 1, 0, 0,1);
                score = 0;
                getSnake();
            }
        }


        public void drawGame() {
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                //Paint paint = new Paint();

                //Background color
                canvas.drawColor(Color.BLACK);

                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(topGap / 2);
                canvas.drawText("Score:" + score + " HighScore:" + high, 10, topGap - 6, paint);

                //draw a border - 4 lines, top right, bottom, left
                paint.setStrokeWidth(3);

                //3 pixel border
                canvas.drawLine(1, topGap, screenWidth - 1, topGap, paint);
                canvas.drawLine(screenWidth - 1, topGap, screenWidth - 1, topGap + (numBlocksHigh * blockSize), paint);
                canvas.drawLine(screenWidth - 1, topGap + (numBlocksHigh * blockSize), 1, topGap + (numBlocksHigh * blockSize), paint);
                canvas.drawLine(1, topGap, 1, topGap + (numBlocksHigh * blockSize), paint);

                //snake
                canvas.drawBitmap(headBitmap, snakeX[0] * blockSize, (snakeY[0] * blockSize) + topGap, paint);

                //body
                for (int i = 1; i < snakeLength - 1; i++) {
                    canvas.drawBitmap(bodyBitmap, snakeX[i] * blockSize, (snakeY[i] * blockSize) + topGap, paint);
                }

                //tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength - 1] * blockSize, (snakeY[snakeLength - 1] * blockSize) + topGap, paint);

                //apple
                canvas.drawBitmap(appleBitmap, appleX * blockSize, (appleY * blockSize) + topGap, paint);
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {

                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {

            }
        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if (motionEvent.getX() >= screenWidth / 2) {
                        //turn right
                        directionOfTravel++;
                        //no such direction
                        if (directionOfTravel == 4)
                            //loop back up to 0
                            directionOfTravel = 0;
                    } else {
                        //turn left
                        directionOfTravel--;
                        if (directionOfTravel == -1) {//no such direction
                            //loop back up to 0
                            directionOfTravel = 3;
                        }
                    }
            }
            return true;
        }
    }
}