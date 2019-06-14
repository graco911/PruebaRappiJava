package com.prueba.rappi.activities;

import android.app.Presentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.pixplicity.easyprefs.library.Prefs;
import com.prueba.rappi.R;
import com.squareup.picasso.Picasso;
import com.takusemba.multisnaprecyclerview.MultiSnapRecyclerView;

import org.w3c.dom.Text;

import javax.inject.Inject;
import javax.inject.Named;

import adapters.MoviesAdapter;
import di.component.ApplicationComponent;
import di.component.DaggerMainActivityComponent;
import di.component.MainActivityComponent;
import di.module.MainActivityContextModule;
import di.qualifier.ActivityContext;
import di.qualifier.ApplicationContext;
import enumerators.EMovieType;
import helpers.Utils;
import interfaces.IServices;
import models.GetMovieResponseData;
import models.GetRequestTokenData;
import models.GetResponseUserData;
import models.GetUserSessionIdData;
import models.SetRequestSessionData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MoviesAdapter.ClickListener {

    @Inject
    @Named("TopRated")
    public MoviesAdapter TopRatedAdapter;

    @Inject
    @Named("Popular")
    public MoviesAdapter PopularAdapter;

    @Inject
    @Named("Upcoming")
    public MoviesAdapter UpcomingAdapter;

    @Inject
    public IServices apiInterface;

    @Inject
    @ApplicationContext
    public Context mContext;

    @Inject
    @ActivityContext
    public Context activityContext;


    private boolean isbBusy;
    private int currentPageTopRated = 1;
    private int currentPageUpcoming = 1;
    private int currentPagePopular = 1;

    private MultiSnapRecyclerView listPopular, listUpcoming, listTopRated;
    private MainActivityComponent mainActivityComponent;
    private CustomTabsServiceConnection connection;
    private View headerLayout;
    private Gson gson;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        headerLayout = navigationView.getHeaderView(0);

        Utils.GetPrefsInstance(this);

        ApplicationComponent applicationComponent = MovieApplication.get(this).getApplicationComponent();
        mainActivityComponent = DaggerMainActivityComponent.builder()
                .mainActivityContextModule(new MainActivityContextModule(this))
                .applicationComponent(applicationComponent)
                .build();

        mainActivityComponent.injectMainActivity(this);

        LinearLayoutManager layoutManagerPopular = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager layoutManagerTopRated = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        LinearLayoutManager layoutManagerUpcoming = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        listPopular = findViewById(R.id.listPopularSnap);
        listPopular.setLayoutManager(layoutManagerPopular);

        listUpcoming = findViewById(R.id.listUpcomingSnap);
        listUpcoming.setLayoutManager(layoutManagerUpcoming);

        listTopRated = findViewById(R.id.listTopRatedSnap);
        listTopRated.setLayoutManager(layoutManagerTopRated);

        GetMovies(EMovieType.upcoming, UpcomingAdapter);
        GetMovies(EMovieType.popular, PopularAdapter);
        GetMovies(EMovieType.top_rated, TopRatedAdapter);

        listTopRated.setAdapter(TopRatedAdapter);
        listUpcoming.setAdapter(UpcomingAdapter);
        listPopular.setAdapter(PopularAdapter);

        SetScrollListener(listTopRated, layoutManagerTopRated, EMovieType.top_rated, TopRatedAdapter, currentPageTopRated);
        SetScrollListener(listPopular, layoutManagerPopular, EMovieType.popular, PopularAdapter, currentPagePopular);
        SetScrollListener(listUpcoming, layoutManagerUpcoming, EMovieType.upcoming, UpcomingAdapter, currentPageUpcoming);

        gson = new Gson();

        ShowUserData();
    }

    private void ShowUserData() {

        if (!Prefs.getString("UserData", "").isEmpty()){

            navigationView.getMenu().findItem(R.id.nav_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.nav_favorites).setVisible(true);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(true);

            GetResponseUserData userData = gson.fromJson(Prefs.getString("UserData", ""), GetResponseUserData.class);

            TextView userName = headerLayout.findViewById(R.id.textUserName);
            userName.setText(userData.getUsername());

            Picasso.with(this).load(String.format("https://www.gravatar.com/avatar/%s", userData.getAvatar().getGravatar().getHash())).into((ImageView) headerLayout.findViewById(R.id.imageView));

            userName.setVisibility(View.VISIBLE);
            headerLayout.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
        }
    }

    private void SetScrollListener(MultiSnapRecyclerView list,
                                   final LinearLayoutManager lm,
                                   final EMovieType movietype,
                                   final MoviesAdapter adapter,
                                   final int pageList) {

        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = lm.getItemCount();
                int visibleItemCount = lm.getChildCount();
                int firstVisibleItem = lm.findFirstVisibleItemPosition();

                if (firstVisibleItem + visibleItemCount >= totalItemCount / 2) {
                    if (!isbBusy) {
                        getMovies(pageList + 1, movietype, adapter);
                    }
                }

            }
        });

    }

    private void getMovies(final int page, EMovieType movietype, final MoviesAdapter adapt) {

        isbBusy = true;

        apiInterface.getMovies(movietype.toString(), Utils.API_KEY, Utils.LANGUAGE, page)
                .enqueue(new Callback<GetMovieResponseData>() {
                    @Override
                    public void onResponse(Call<GetMovieResponseData> call, Response<GetMovieResponseData> response) {
                        adapt.setData(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<GetMovieResponseData> call, Throwable t) {

                    }
                });

    }

    private void GetMovies(EMovieType movieType, final MoviesAdapter adapter) {

        apiInterface.getMovies(movieType.toString(), Utils.API_KEY, Utils.LANGUAGE, Utils.PAGE)
                .enqueue(new Callback<GetMovieResponseData>() {
                    @Override
                    public void onResponse(Call<GetMovieResponseData> call, Response<GetMovieResponseData> response) {
                        adapter.setData(response.body().getResults());
                    }

                    @Override
                    public void onFailure(Call<GetMovieResponseData> call, Throwable t) {

                    }
                });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id){
            case R.id.nav_login:
                LoginUser();
                break;
            case R.id.nav_favorites:
                break;
            case R.id.nav_options:
                break;
            case R.id.nav_logout:
                LogoutUser();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    private void LogoutUser() {

        Prefs.putString("UserData", "");
        Prefs.putString("UserSession", "");
        finish();
    }

    private void LoginUser() {

        apiInterface.getRequestToken(Utils.API_KEY)
                .enqueue(new Callback<GetRequestTokenData>() {
                    @Override
                    public void onResponse(Call<GetRequestTokenData> call, Response<GetRequestTokenData> response) {

                        if (response.body().getSuccess())
                        {
                            response.body().getRequestToken();

                            final String parseUrl = String.format("https://www.themoviedb.org/authenticate/%s?redirect_to=anything://com.recargas.auth.auth_callback_anything", response.body().getRequestToken());

                            connection = new CustomTabsServiceConnection() {
                                @Override
                                public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient client) {
                                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                                    CustomTabsIntent intent = builder.build();
                                    client.warmup(0L); // This prevents backgrounding after redirection
                                    intent.launchUrl(MainActivity.this, Uri.parse(parseUrl));
                                }

                                @Override
                                public void onServiceDisconnected(ComponentName name) {

                                }
                            };

                            CustomTabsClient.bindCustomTabsService(getApplicationContext(), "com.android.chrome", connection);

                        }
                    }

                    @Override
                    public void onFailure(Call<GetRequestTokenData> call, Throwable t) {

                    }
                });
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        String action = intent.getAction();
        String data = intent.getDataString();
        if (Intent.ACTION_VIEW.equals(action) && data != null) {

            String result = data.substring(data.indexOf("=") + 1, data.indexOf("&"));

            if (!result.isEmpty()){

                SetRequestSessionData request = new SetRequestSessionData();
                request.setRequestToken(result);
                apiInterface.getSession(Utils.API_KEY, request)
                        .enqueue(new Callback<GetUserSessionIdData>() {
                            @Override
                            public void onResponse(Call<GetUserSessionIdData> call, Response<GetUserSessionIdData> response)
                            {
                                if (response.body().getSuccess()){

                                    Prefs.putString("UserSession", response.body().getSessionId());

                                    GetUserData(response.body().getSessionId());
                                }
                            }

                            @Override
                            public void onFailure(Call<GetUserSessionIdData> call, Throwable t) {

                            }
                        });
            }
        }
    }

    private void GetUserData(String sessionId) {

        apiInterface.getUserData(Utils.API_KEY, sessionId)
                .enqueue(new Callback<GetResponseUserData>() {
                    @Override
                    public void onResponse(Call<GetResponseUserData> call, Response<GetResponseUserData> response) {
                        Prefs.putString("UserData", gson.toJson(response.body()));

                        ShowUserData();
                    }

                    @Override
                    public void onFailure(Call<GetResponseUserData> call, Throwable t) {

                    }
                });

    }

    @Override
    public void launchIntent(int movieId, ImageView movieImage) {

        Intent intent = new Intent(this, MovieDetailActivity.class);
        intent.putExtra("MovieId", movieId);
        ActivityOptionsCompat options = ActivityOptionsCompat.
                makeSceneTransitionAnimation(this, movieImage, "movieDetail");
        startActivity(intent, options.toBundle());
    }
}
