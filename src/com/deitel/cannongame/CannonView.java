// CannonView.java
// Displays the Cannon Game
package com.deitel.cannongame;

import java.util.HashMap;
import java.util.Map;

import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.Random;

public class CannonView extends SurfaceView 
   implements SurfaceHolder.Callback
{
   private CannonThread cannonThread; // controls the game loop
   private Activity activity; // to display Game Over dialog in GUI thread
   private boolean dialogIsDisplayed = false;   
               
   private int numberOfReflections; //to be able to count ball reflections 

   // variables for the game loop and tracking statistics
   private boolean gameOver; // is the game over?
   private double timeLeft; // the amount of time left in seconds
   private int shotsFired; // the number of shots the user has fired
   
   private double totalElapsedTime; // the number of seconds elapsed
   private Line target; // start and end points of the target
   private int targetDistance; // target distance from left
   private int targetBeginning; // target distance from top
   private int targetEnd; // target bottom's distance from top
   private int initialTargetVelocity; // initial target speed multiplier
   private double targetVelocityX; // target speed multiplier during game
   private double targetVelocityY; // target speed multiplier during game

   private int lineWidth; // width of the target and blocker
   private int screenWidth; // width of the screen
   private int screenHeight; // height of the screen

   // constants and variables for managing sounds
   private static final int TARGET_SOUND_ID = 0;
   private static final int CANNON_SOUND_ID = 1;
   private static final int BLOCKER_SOUND_ID = 2;
   private SoundPool soundPool; // plays sound effects
   private Map<Integer, Integer> soundMap; // maps IDs to SoundPool

   // Paint variables used when drawing each item on the screen
   private Paint textPaint; // Paint used to draw text
   private Paint textPaintReflections; // Paint used to draw text
   private Paint targetPaint; // Paint used to draw the target
   private Paint backgroundPaint; // Paint used to clear the drawing area

   // public constructor
   public CannonView(Context context, AttributeSet attrs)
   {
      super(context, attrs); // call super's constructor
      activity = (Activity) context; 
      
      // register SurfaceHolder.Callback listener
      getHolder().addCallback(this); 
      target = new Line(); // create the target as a Line
      // initialize SoundPool to play the app's three sound effects
      soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

      // create Map of sounds and pre-load sounds
      soundMap = new HashMap<Integer, Integer>(); // create new HashMap
      soundMap.put(TARGET_SOUND_ID,
         soundPool.load(context, R.raw.target_hit, 1));
      soundMap.put(CANNON_SOUND_ID,
         soundPool.load(context, R.raw.cannon_fire, 1));
      soundMap.put(BLOCKER_SOUND_ID,
         soundPool.load(context, R.raw.blocker_hit, 1));
      textPaint = new Paint(); // Paint for drawing text
      textPaintReflections = new Paint(); // Paint for drawing text
      targetPaint = new Paint(); // Paint for drawing the target
      backgroundPaint = new Paint(); // Paint for drawing the target background
   } // end CannonView constructor

   // called by surfaceChanged when the size of the SurfaceView changes,
   // such as when it's first added to the View hierarchy
   @Override
   protected void onSizeChanged(int w, int h, int oldw, int oldh)
   {
      super.onSizeChanged(w, h, oldw, oldh);

      screenWidth = w; // store the width
      screenHeight = h; // store the height
      lineWidth = w / 32; // target and blocker 1/24 screen width
      targetDistance = w * 7 / 8; // target 7/8 screen width from left
      targetBeginning = h / 8; // distance from top 1/8 screen height
      targetEnd = h * 5 / 32; 
      initialTargetVelocity = h/2; // initial target speed 
      target.start = new Point(targetDistance, targetBeginning);
      target.end = new Point(targetDistance, targetEnd);
      textPaint.setTextSize(w / 20); // text size 1/20 of screen width
      textPaint.setAntiAlias(true); // smoothes the text
      textPaint.setColor(Color.RED);// set background color
      
      textPaintReflections.setTextSize(w / 30); // text size 1/20 of screen width



      newGame(); // set up and start a new game
   } // end method onSizeChanged

   // reset all the screen elements and start a new game
   public void newGame()
   {
      textPaintReflections.setColor(Color.YELLOW);// set background color
      textPaintReflections.setAntiAlias(true); // smoothes the text
      targetPaint.setStrokeWidth(lineWidth); // set line thickness       
      backgroundPaint.setColor(Color.GRAY); 
	   
	  numberOfReflections = 0;
      // set every element of hitStates to false--restores target pieces
 
      Random rand = new Random();
      
  	  int randomNumberAngle = rand.nextInt(360);
  	  double multiplierX = Math.sin(randomNumberAngle);
  	  double multiplierY = Math.cos(randomNumberAngle);
  	  
      targetVelocityX = initialTargetVelocity * multiplierX;
      targetVelocityY = initialTargetVelocity * multiplierY;
      
 
      timeLeft = 10; 
      shotsFired = 0; // set the initial number of shots fired
      totalElapsedTime = 0.0; // set the time elapsed to zero
      
      target.start.set(targetDistance, targetBeginning);
      target.end.set(targetDistance, targetEnd);
      
      if (gameOver)
      {
         gameOver = false; // the game is not over
         cannonThread = new CannonThread(getHolder());
         cannonThread.start();
      } // end if
   } 

   private void updatePositions(double elapsedTimeMS)
   {
      double interval = elapsedTimeMS / 1000.0; // convert to seconds
      
      double targetUpdateX = interval * targetVelocityX;
      double targetUpdateY = interval * targetVelocityY;
      
      target.start.x += targetUpdateX;
      target.start.y += targetUpdateY;
      target.end.x += targetUpdateX;
      target.end.y += targetUpdateY;
      if (target.start.y < 0.00 || target.end.y > screenHeight){
    	  targetVelocityY *= -1;
    	  numberOfReflections += 1;
      }
      if (target.start.x < 0.00 || target.end.x > screenWidth){
    	  targetVelocityX *= -1; 
    	  numberOfReflections += 1;
      }   

      timeLeft -= interval; // subtract from time left
      if (timeLeft <= 0.0 || numberOfReflections >= 10)
      {  timeLeft = 0.0;
         gameOver = true; // the game is over
         cannonThread.setRunning(false);
         showGameOverDialog(R.string.lose); // show the losing dialog
      } // end if
   } // end method updatePositions

   // fires a cannonball
   public void fireCannonball(MotionEvent event)
   { 
	   
   }
   
   public void alignCannon(MotionEvent event)
   {
	   
   } 

   // draws the game to the given Canvas
   public void drawGameElements(Canvas canvas)
   {
	   String testString = String.valueOf(numberOfReflections);
      // clear the background
      canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), 
         backgroundPaint);
      
      // display time remaining
      canvas.drawText(getResources().getString(
      R.string.time_remaining_format, timeLeft), 30, 50, textPaint);
      canvas.drawText(testString, 30, 90, textPaintReflections);      


      targetPaint.setColor(Color.BLUE);
      canvas.drawLine(target.start.x, target.start.y, target.end.x, target.end.y, targetPaint);
    }

   private void showGameOverDialog(int messageId)
   {
      // create a dialog displaying the given String
      final AlertDialog.Builder dialogBuilder =  new AlertDialog.Builder(getContext());
      dialogBuilder.setTitle(getResources().getString(messageId));
      dialogBuilder.setCancelable(false);

      // display number of shots fired and total time elapsed
      dialogBuilder.setMessage(getResources().getString(
         R.string.results_format, shotsFired, totalElapsedTime));
      dialogBuilder.setPositiveButton(R.string.reset_game,
         new DialogInterface.OnClickListener()
         {
            // called when "Reset Game" Button is pressed
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
               dialogIsDisplayed = false;
               newGame(); // set up and start a new game
            } // end method onClick
         } // end anonymous inner class
      ); // end call to setPositiveButton

      activity.runOnUiThread(
         new Runnable() {
            public void run()
            {
               dialogIsDisplayed = true;
               dialogBuilder.show(); // display the dialog
            } // end method run
         } // end Runnable
      ); // end call to runOnUiThread
   } // end method showGameOverDialog

   // stops the game
   public void stopGame()
   {
      if (cannonThread != null)
         cannonThread.setRunning(false);
   } // end method stopGame

   // releases resources; called by CannonGame's onDestroy method 
   public void releaseResources()
   {
      soundPool.release(); // release all resources used by the SoundPool
      soundPool = null; 
   } // end method releaseResources

   // called when surface changes size
   @Override
   public void surfaceChanged(SurfaceHolder holder, int format,
      int width, int height)
   {
   } // end method surfaceChanged

   // called when surface is first created
   @Override
   public void surfaceCreated(SurfaceHolder holder)
   {
      if (!dialogIsDisplayed)
      {
         cannonThread = new CannonThread(holder);
         cannonThread.setRunning(true);
         cannonThread.start(); // start the game loop thread
      } // end if
   } // end method surfaceCreated

   // called when the surface is destroyed
   @Override
   public void surfaceDestroyed(SurfaceHolder holder)
   {
      // ensure that thread terminates properly
      boolean retry = true;
      cannonThread.setRunning(false);
      
      while (retry)
      {
         try
         {
            cannonThread.join();
            retry = false;
         } // end try
         catch (InterruptedException e)
         {
         } // end catch
      } // end while
   } // end method surfaceDestroyed
   
   // Thread subclass to control the game loop
   private class CannonThread extends Thread
   {
      private SurfaceHolder surfaceHolder; // for manipulating canvas
      private boolean threadIsRunning = true; // running by default
      
      // initializes the surface holder
      public CannonThread(SurfaceHolder holder)
      {
         surfaceHolder = holder;
         setName("CannonThread");
      } // end constructor
      
      // changes running state
      public void setRunning(boolean running)
      {
         threadIsRunning = running;
      } // end method setRunning
      
      // controls the game loop
      @Override
      public void run()
      {
         Canvas canvas = null; // used for drawing
         long previousFrameTime = System.currentTimeMillis(); 
        
         while (threadIsRunning)
         {
            try
            {
               canvas = surfaceHolder.lockCanvas(null);               
               // lock the surfaceHolder for drawing
               synchronized(surfaceHolder)
               {   
                  long currentTime = System.currentTimeMillis();
                  double elapsedTimeMS = currentTime - previousFrameTime;
                  totalElapsedTime += elapsedTimeMS / 1000.00; 
                  updatePositions(elapsedTimeMS); // update game state
                  drawGameElements(canvas); // draw 
                  previousFrameTime = currentTime; // update previous time
               } // end synchronized block
            } // end try
            finally
            {
               if (canvas != null) 
                  surfaceHolder.unlockCanvasAndPost(canvas);
            } // end finally
         } // end while
      } // end method run
   } // end nested class CannonThread
} // end class CannonView

