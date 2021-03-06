package fr.iut63.a2ddicegameupdate.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fr.iut63.a2ddicegameupdate.R;
import fr.iut63.a2ddicegameupdate.models.game.GameDrawer;
import fr.iut63.a2ddicegameupdate.models.game.GameState;
import fr.iut63.a2ddicegameupdate.models.loop.Loop;
import fr.iut63.a2ddicegameupdate.models.map.MapGeneration;
import fr.iut63.a2ddicegameupdate.models.player.AvatarMovement;
import fr.iut63.a2ddicegameupdate.models.serialization.PersistenceManagerBinary;
import fr.iut63.a2ddicegameupdate.models.serialization.ResultSerializable;
import fr.iut63.a2ddicegameupdate.models.serialization.ScoreRankSerializable;

/**
 * Acivité lancé lors du lancement du jeu.
 */
public class Play extends Activity
{
    private final DisplayMetrics displayMetrics = new DisplayMetrics();
    private ConstraintLayout constraintLayout;
    private int height, width;

    private GameDrawer gameDrawer;
    private Loop loop = new Loop();
    private Button button_roll_dice;
    private Button updater_timer;
    private int difficulty;

    private MapGeneration map;

    private ImageView imgPerso;

    /**
     * Lancement de la vue du jeu, génération de la map et du personnage, création des boutons du jeu.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_panel);
        button_roll_dice = findViewById(R.id.button_roll_dice);
        updater_timer = findViewById(R.id.button_update_timer);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        height = displayMetrics.heightPixels;
        width = displayMetrics.widthPixels;

        difficulty = 1;
        int avatar = 1;

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            difficulty = extras.getInt("difficulty");
            avatar = extras.getInt("avatar");
        }
        constraintLayout = findViewById(R.id.constLayoutGame);

        map = new MapGeneration(width, height, difficulty);
        gameDrawer = new GameDrawer(this, map);
        gameDrawer.drawMap(difficulty);
        GameState game = new GameState();

        startLoop();
        imgPerso = gameDrawer.drawPlayer(avatar);

        AvatarMovement avatarMovement = new AvatarMovement();

        Play play = this;

        button_roll_dice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_roll_dice.setClickable(false);
                avatarMovement.avatarMovement(imgPerso, map, loop, play);
                endGame();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        button_roll_dice.setClickable(true);
                    }
                });
                thread.start();

            }
        });

        updater_timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updater_timer.setText("Time : " + loop.getTime());
            }
        });

    }

    /**
     * Méthode appelé lors de la fin du jeu.
     */
    public void endGame(){
        if (!loop.isRunning()) {
            Button button_update_timer = findViewById(R.id.button_update_timer);
            button_update_timer.setOnClickListener(view -> {
                button_update_timer.setText("Time : " + loop.getTime());
            });

            button_roll_dice.setEnabled(false);

            TextView textView = new TextView(this);
            textView.setText(R.string.end_game);
            textView.setTextSize(40);
            textView.setTextColor(getResources().getColor(R.color.black));
            textView.setX((int) Math.ceil(width / 2) - (width / 7));
            textView.setY((int) Math.ceil(height / 4));
            constraintLayout.addView(textView);

            EditText editText = new EditText(this);
            editText.setHint(R.string.enter_name);
            editText.setTextSize(20);
            editText.setTextColor(getResources().getColor(R.color.black));
            editText.setX((int) Math.ceil(width / 2) - (width / 7));
            editText.setY((int) Math.ceil(height / 2));
            constraintLayout.addView(editText);

            Button button = new Button(this);
            button.setText(R.string.menu);
            button.setTextSize(40);
            button.setTextColor(getResources().getColor(R.color.black));
            button.setX((int) Math.ceil(width / 2) - (width / 10));
            button.setY((int) Math.ceil(height / 4) + (width / 4));

            constraintLayout.addView(button);

            button.setOnClickListener(view -> {
                String name = String.valueOf(editText.getText());
                if(name.equals("")) name = "Default";
                ResultSerializable re = new ResultSerializable(name, difficulty, 15000-(difficulty*loop.getTime()), loop.getTime());
                PersistenceManagerBinary pers = new PersistenceManagerBinary();
                ScoreRankSerializable score = pers.load(this);
                score.addResult(re);
                List<ResultSerializable> list = score.getRank();
                Collections.sort(list, new Comparator<ResultSerializable>() {
                    @Override
                    public int compare(ResultSerializable p1, ResultSerializable p2) {
                        return p1.getScore() + p2.getScore();
                    }
                });
                score.setRank(list);
                pers.save(score, this);
                finish();
            });
        }

    }

    public void startLoop() {
        loop = new Loop();
        loop.setRunning(true);
        Thread boucleThread = new Thread(loop);
        boucleThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent i;

        switch (item.getItemId())
        {
            case R.id.menuAbout:
                i = new Intent(this, ScoresActivity.class);
                startActivity(i);
                return true;
            case R.id.menuExit:
                finish();
                return true;
        }

        return false;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public ConstraintLayout getConstraintLayout() {
        return constraintLayout;
    }

}