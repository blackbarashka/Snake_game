package com.tythen.tysnake;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.tythen.tysnake.Constant.*;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private String direction = "right";
    private int foodX = 0, foodY = 0;
    private double currentValue = 0;
    private int foodX1 = 0, foodY1 = 0;
    private int foodX2 = 0, foodY2 = 0;
    private int score = 0;
    private int spid = 0;
    private TextView tv_score;
    private List<SnakePoint> snakePoints = new ArrayList();
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private Timer timer;
    private Canvas canvas = null;
    private Paint pointColor = null;
    private boolean gameOver;
    private SoundPool soundPool;
    private MediaPlayer mediaPlayer;
    private int eatingSoundId;
    private int deadSoundId;


    private double kp;
    private double ki;
    private double kd;

    private double setpoint;
    private double prevError;
    private double integral;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        surfaceView = findViewById(R.id.sv_game);
        surfaceView.getHolder().addCallback(this);
        tv_score = findViewById(R.id.tv_score);

        Button btn_up = findViewById(R.id.btn_up);
        Button btn_right = findViewById(R.id.btn_right);
        Button btn_left = findViewById(R.id.btn_left);
        Button btn_down = findViewById(R.id.btn_down);

        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("down")) {
                    direction = "up";
                }
            }
        });
        btn_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("left")) {
                    direction = "right";
                }
            }
        });
        btn_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("right")) {
                    direction = "left";
                }
            }
        });
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!direction.equals("up")) {
                    direction = "down";
                }
            }
        });



        //音乐
        mediaPlayer = MediaPlayer.create(this, R.raw.bgm);
        mediaPlayer.setLooping(true);
        //音效
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .build();

        eatingSoundId = soundPool.load(this, R.raw.eating, 1);
        deadSoundId = soundPool.load(this,R.raw.dead,1);
    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        soundPool.release();
    }
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        init();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    private void init() {

        snakePoints.clear();
        score = 0;
        tv_score.setText("0");
        direction = "right";
        int startX = 3 * pointSize;
        for (int i = 0; i < defaultTablePoints; i++) {
            SnakePoint snakePoint = new SnakePoint(startX, pointSize);
            snakePoints.add(snakePoint); //添加蛇的点
            startX -= 2 * pointSize;
        }
        PIDController(1.0, 0.1, 0.01, 0);
        addPoint();
        addPoint_1();
        addPoint_2();
        moveSnake();
        mediaPlayer.start();
    }

    private void addPoint() {
        int newFoodX = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//左右两边各自留一个半径，然后除以半径得到一共有多少个点，然后随机生成一个点
        int newFoodY = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX % 2 != 0) {
            newFoodX++;
        }
        if (newFoodY % 2 != 0) {
            newFoodY++;
        }
        foodX = (newFoodX * pointSize) + pointSize;
        foodY = (newFoodY * pointSize) + pointSize;
    }
    private void addPoint_1() {
        int newFoodX1 = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//
        int newFoodY1 = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX1 % 2 != 0) {
            newFoodX1++;//
        }
        if (newFoodY1 % 2 != 0) {
            newFoodY1++;
        }
        foodX1 = (newFoodX1 * pointSize) + pointSize;
        foodY1 = (newFoodY1 * pointSize) + pointSize;
    }


    private void addPoint_2() {
        int newFoodX2 = new Random().nextInt((surfaceView.getWidth() - 2 * pointSize) / pointSize);//
        int newFoodY2 = new Random().nextInt((surfaceView.getHeight() - 2 * pointSize) / pointSize);
        if (newFoodX2 % 2 != 0) {
            newFoodX2++;
        }
        if (newFoodY2 % 2 != 0) {
            newFoodY2++;
        }
        if(newFoodY2 > 20){
            newFoodY2 = 57;
        }
        else{
            newFoodY2 = 1;
        }
        if(newFoodX2 > 20 || newFoodY2 == 1){
            newFoodX2 = 45;
        }
        else{
            newFoodX2 = 1;
        }
        if((newFoodX2 * pointSize) + pointSize == foodX2 && (newFoodY2 * pointSize) + pointSize == foodY2){
            newFoodY2 = 58 - foodX2;
        }
        foodX2 = (newFoodX2 * pointSize) + pointSize;
        foodY2 = (newFoodY2 * pointSize) + pointSize;
    }

    private void drawCartoonSnakeBody(Canvas canvas, int x, int y, int pointSize) {
        Paint bodyPaint = new Paint();
        bodyPaint.setShader(new LinearGradient(x - pointSize, y - pointSize, x + pointSize, y + pointSize,
                Color.parseColor("#FF6F61"), Color.parseColor("#FFD166"), Shader.TileMode.MIRROR));
        bodyPaint.setAntiAlias(true);

        RectF bodyRect = new RectF(x - pointSize, y - pointSize, x + pointSize, y + pointSize);
        canvas.drawRoundRect(bodyRect, 10, 10, bodyPaint);
    }
    private void moveSnake() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //
                int headPositionX = snakePoints.get(0).getPositionX();
                int headPositionY = snakePoints.get(0).getPositionY();


                if (foodX < headPositionX+size && foodX > headPositionX-size && foodY > headPositionY-size && foodY < headPositionY+size) {
                    growSnake();
                    addPoint();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                int headPositionX1 = snakePoints.get(0).getPositionX();
                int headPositionY1 = snakePoints.get(0).getPositionY();


                if (foodX1 < headPositionX1+size && foodX1 > headPositionX1-size && foodY1 > headPositionY1-size && foodY1 < headPositionY1+size) {
                    growSnake();
                    addPoint_1();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                int headPositionX2 = snakePoints.get(0).getPositionX();
                int headPositionY2 = snakePoints.get(0).getPositionY();


                if (foodX2 < headPositionX2+size && foodX2 > headPositionX2-size && foodY2 > headPositionY2-size && foodY2 < headPositionY2+size) {
                    growSnake();
                    growSnake();
                    addPoint_2();
                    soundPool.play(eatingSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }

                double dt = 0.02;
                double nums = score / 40.0;
                setpoint = Math.min(nums,30);
                double output = calculate(currentValue, dt);
                currentValue += output * dt; //
                spid = 10 * (int)currentValue;
                System.out.println(currentValue);
                switch (direction) {
                    case "right":
                        snakePoints.get(0).setPositionX(headPositionX + speed + spid);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "left":
                        snakePoints.get(0).setPositionX(headPositionX - speed - spid);
                        snakePoints.get(0).setPositionY(headPositionY);
                        break;
                    case "up":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY - speed - spid);
                        break;
                    case "down":
                        snakePoints.get(0).setPositionX(headPositionX);
                        snakePoints.get(0).setPositionY(headPositionY + speed + spid);
                        break;
                }

                if (checkGameOver(headPositionX, headPositionY)) {

                    timer.purge();
                    timer.cancel();

                    soundPool.play(deadSoundId,1,1,1,0,1);
                    mediaPlayer.pause();

                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                    builder.setTitle("Игра окончена");
                    builder.setCancelable(false);//
                    builder.setMessage("Ваш результат：" + score);
                    saveScore();
                    builder.setNegativeButton("Назад", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            startActivity(new Intent(GameActivity.this,MainActivity.class));
                        }
                    });
                    builder.setPositiveButton("Заново", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            init();
                        }
                    });


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.show();
                        }
                    });
                    return;
                }
                else
                {

                    canvas = surfaceHolder.lockCanvas();
                    canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);//


                    int X = snakePoints.get(0).getPositionX();
                    int Y = snakePoints.get(0).getPositionY();
                    canvas.drawCircle(X, Y, pointSize, createPointColor());
                    drawCartoonSnakeBody(canvas, X, Y,pointSize);

                    switch (direction) {
                        case "right":
                            canvas.drawCircle(X+30, Y-10, 10, createEyeColor());
                            canvas.drawCircle(X+30, Y+10, 10, createEyeColor());
                            canvas.drawCircle(X+32, Y-10, 2, createBlackColor());
                            canvas.drawCircle(X+32, Y+10, 2, createBlackColor());
                            break;
                        case "left":
                            canvas.drawCircle(X-30, Y-10, 10, createEyeColor());
                            canvas.drawCircle(X-30, Y+10, 10, createEyeColor());
                            canvas.drawCircle(X-32, Y-10, 2, createBlackColor());
                            canvas.drawCircle(X-32, Y+10, 2, createBlackColor());
                            break;
                        case "up":
                            canvas.drawCircle(X+10, Y-30, 10, createEyeColor());
                            canvas.drawCircle(X-10, Y-30, 10, createEyeColor());
                            canvas.drawCircle(X+10, Y-32,2, createBlackColor());
                            canvas.drawCircle(X-10, Y-32,  2, createBlackColor());
                            break;
                        case "down":
                            canvas.drawCircle(X+10, Y+30, 10, createEyeColor());
                            canvas.drawCircle(X-10, Y+30, 10, createEyeColor());
                            canvas.drawCircle(X+10, Y+32,2, createBlackColor());
                            canvas.drawCircle(X-10, Y+32, 2, createBlackColor());
                            break;
                    }


                    Paint foodPaint = new Paint();
                    foodPaint.setColor(Color.parseColor("#FF3B3B")); //
                    foodPaint.setAntiAlias(true);
                    canvas.drawCircle(foodX, foodY, 20, foodPaint);


                    Paint leafPaint = new Paint();
                    leafPaint.setColor(Color.parseColor("#4CAF50")); //
                    leafPaint.setAntiAlias(true);
                    canvas.drawRect(foodX - 10, foodY - 30, foodX + 10, foodY - 20, leafPaint); // 叶子


                    Paint highlightPaint = new Paint();
                    highlightPaint.setColor(Color.parseColor("#FFEB3B")); //
                    highlightPaint.setAntiAlias(true);

                    canvas.drawCircle(foodX + 10, foodY - 10, 5, highlightPaint);





                    Paint starPaint = new Paint();
                    starPaint.setColor(Color.parseColor("#FFD700")); //
                    starPaint.setAntiAlias(true);
                    starPaint.setStyle(Paint.Style.FILL); // 填


                    float centerX = foodX2; //
                    float centerY = foodY2; //
                    float outerRadius = 40; //
                    float innerRadius = 20; //


                    Path starPath = new Path();
                    double angle = Math.toRadians(-18); //

                    for (int i = 0; i < 5; i++) {
                        //
                        float outerX = (float) (centerX + outerRadius * Math.cos(angle));
                        float outerY = (float) (centerY + outerRadius * Math.sin(angle));
                        if (i == 0) {
                            starPath.moveTo(outerX, outerY);
                        } else {
                            starPath.lineTo(outerX, outerY);
                        }

                        //
                        angle += Math.toRadians(36); //
                        float innerX = (float) (centerX + innerRadius * Math.cos(angle));
                        float innerY = (float) (centerY + innerRadius * Math.sin(angle));
                        starPath.lineTo(innerX, innerY); //

                        angle += Math.toRadians(36); //
                    }

                    starPath.close(); //

//
                    canvas.drawPath(starPath, starPaint);

// （）
                    Paint highlightPaints = new Paint();
                    highlightPaints.setColor(Color.parseColor("#FFFFFF")); // 高光颜色（白色）
                    highlightPaints.setAntiAlias(true);
                    highlightPaints.setAlpha(128); // 半透明效果

//
                    canvas.drawCircle(centerX + 15, centerY - 15, 10, highlightPaints);

                    Paint foodPaint1 = new Paint();
                    foodPaint1.setColor(Color.parseColor("#FF3B3B")); // 红色
                    foodPaint1.setAntiAlias(true);
                    canvas.drawCircle(foodX1, foodY1, 20, foodPaint1);

                    //
                    Paint leafPaint1 = new Paint();
                    leafPaint1.setColor(Color.parseColor("#4CAF50")); // 绿色
                    leafPaint1.setAntiAlias(true);
                    canvas.drawRect(foodX1 - 10, foodY1 - 30, foodX1 + 10, foodY1 - 20, leafPaint1); // 叶子

                    //
                    Paint highlightPaint1 = new Paint();
                    highlightPaint1.setColor(Color.parseColor("#FFEB3B")); // 黄色
                    highlightPaint1.setAntiAlias(true);

                    canvas.drawCircle(foodX1 + 10, foodY1 - 10, 5, highlightPaint1);

                    //
                    for (int i = 1; i < snakePoints.size(); i++) {
                        //
                        int tempX = snakePoints.get(i).getPositionX();
                        int tempY = snakePoints.get(i).getPositionY();

                        //
                        snakePoints.get(i).setPositionX(headPositionX);
                        snakePoints.get(i).setPositionY(headPositionY);

                        // 按照新位置绘制
                        int indexX = snakePoints.get(i).getPositionX();
                        int indexY = snakePoints.get(i).getPositionY();
                        drawCartoonSnakeBody(canvas, indexX, indexY, pointSize);

                        //
                        if (tempX != 0) {
                            int midX = (tempX + headPositionX) / 2;
                            int midY = (tempY + indexY) / 2;
                            drawCartoonSnakeBody(canvas, midX, midY, pointSize);
                        }

                        //
                        headPositionX = tempX;
                        headPositionY = tempY;
                    }
                }

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }, snakeMovingSpeed , snakeMovingSpeed);

    }

    private void saveScore() {
        SharedPreferences shared = getSharedPreferences("score", Context.MODE_PRIVATE);
        int total = shared.getInt("total",0);
        ++total;
        SharedPreferences.Editor editor = shared.edit();
        Date date = new Date(System.currentTimeMillis());
        String nowDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);

        editor.putString(String.valueOf(total+"date"),nowDate);
        editor.putInt(String.valueOf(total+"score"),score);
        editor.putInt("total",total);
        editor.apply();
    }
    public void PIDController(double kp, double ki, double kd, double setpoint) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.setpoint = setpoint;
        this.prevError = 0;
        this.integral = 0;
    }

    public double calculate(double currentValue, double dt) {

        double error = setpoint - currentValue;


        double proportional = kp * error;


        integral += error * dt;
        double integralTerm = ki * integral;


        double derivative = (error - prevError) / dt;
        double derivativeTerm = kd * derivative;


        prevError = error;


        double output = proportional + integralTerm + derivativeTerm;

        return output;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
        this.integral = 0; //
        this.prevError = 0; //
    }


    private void growSnake() {

        snakePoints.add(new SnakePoint(0, 0));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                score++;
                tv_score.setText(String.valueOf(score));
            }
        });
    }

    private boolean checkGameOver(int headPositionX, int headPositionY) {
        boolean gameOver = false;
        if (snakePoints.get(0).getPositionX() < 0 ||
                snakePoints.get(0).getPositionX() > surfaceView.getWidth() ||
                snakePoints.get(0).getPositionY() < 0 ||
                snakePoints.get(0).getPositionY() > surfaceView.getHeight()) {
            gameOver = true;
        }
            for (int i = 1; i < snakePoints.size(); i++) {
                if (snakePoints.get(i).getPositionX() == headPositionX && snakePoints.get(i).getPositionY() == headPositionY) {
                    gameOver = true;
                }
            }

        return gameOver;
    }

    @SuppressLint("ResourceAsColor")
    private Paint createPointColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.GREEN);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.GREEN);
        return pointColor;
    }

    @SuppressLint("ResourceAsColor")
    private Paint createFoodColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLUE);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.RED);
        return pointColor;
    }
    @SuppressLint("ResourceAsColor")
    private Paint createEyeColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLUE);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.YELLOW);
        return pointColor;
    }
    @SuppressLint("ResourceAsColor")
    private Paint createBlackColor() {
        if (pointColor == null) {
            pointColor = new Paint();
            pointColor.setColor(Color.BLACK);
            pointColor.setStyle(Paint.Style.FILL);
            pointColor.setAntiAlias(true);
        }
        pointColor.setColor(Color.BLACK);
        return pointColor;
    }


}