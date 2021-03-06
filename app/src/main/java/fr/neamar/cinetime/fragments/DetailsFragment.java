package fr.neamar.cinetime.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import fr.neamar.cinetime.R;
import fr.neamar.cinetime.api.APIHelper;
import fr.neamar.cinetime.callbacks.TaskMoviesCallbacks;
import fr.neamar.cinetime.fragments.MoviesFragment.Callbacks;
import fr.neamar.cinetime.objects.Movie;

public class DetailsFragment extends Fragment implements TaskMoviesCallbacks {

    public static final String ARG_ITEM_ID = "item_id";
    public static final String ARG_THEATER_NAME = "theater_name";
    static public Movie displayedMovie;
    private static boolean toFinish = false;
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(int position, Fragment source, View currentView) {

        }

        @Override
        public void setFragment(Fragment fragment) {
        }

        @Override
        public void setIsLoading(Boolean isLoading) {
        }

        @Override
        public void finishNoNetwork() {
            toFinish = true;
        }
    };
    protected String theater = "";
    private Callbacks mCallbacks = sDummyCallbacks;
    private TextView title;
    private TextView extra;
    private TextView display;
    private ImageView poster;
    private TextView certificate;
    private TextView synopsis;
    private RatingBar pressRating;
    private TextView pressRatingText;
    private RatingBar userRating;
    private TextView userRatingText;
    private LoadMovieTask mTask;
    private boolean titleToSet = false;
    private ProgressDialog dialog = null;

    public DetailsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (displayedMovie != null && displayedMovie.synopsis.equalsIgnoreCase("") && mTask == null) {
            mTask = new LoadMovieTask(this);
            mTask.execute(displayedMovie.code);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);
        title = view.findViewById(R.id.details_title);
        extra = view.findViewById(R.id.details_extra);
        display = view.findViewById(R.id.details_display);
        poster = view.findViewById(R.id.details_poster);
        pressRating = view.findViewById(R.id.details_pressrating);
        pressRatingText = view.findViewById(R.id.details_pressrating_text);
        userRating = view.findViewById(R.id.details_userrating);
        userRatingText = view.findViewById(R.id.details_userrating_text);
        synopsis = view.findViewById(R.id.details_synopsis);
        certificate = view.findViewById(R.id.details_certificate);
        if (displayedMovie != null) {
            updateUI();
        }
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        mCallbacks = (Callbacks) activity;
        mCallbacks.setFragment(this);
        if (titleToSet) {
            getActivity().setTitle(displayedMovie.title);
            titleToSet = false;
        }
        if (toFinish) {
            mCallbacks.finishNoNetwork();
            toFinish = false;
        }
    }

    public void shareMovie() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, displayedMovie.getSharingText(theater));

        startActivity(Intent.createChooser(sharingIntent, "Partager le film..."));
    }

    public void displayTrailer() {
        class RetrieveTrailerTask extends AsyncTask<Movie, Void, String> {
            @Override
            protected String doInBackground(Movie... movies) {
                return new APIHelper().downloadTrailerUrl(movies[0]);
            }

            @Override
            protected void onPostExecute(String trailerUrl) {

                // Dismiss dialog
                if (dialog != null) {
                    try {
                        dialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (trailerUrl == null && getActivity() != null) {
                    Toast.makeText(getActivity(), "Woops ! La bande annonce ne semble pas disponible...", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.parse(trailerUrl), "video/mp4");
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getActivity(), "Vous devez avoir un lecteur vidéo pour afficher la bande-annonce de ce film.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        new RetrieveTrailerTask().execute(DetailsFragment.displayedMovie);
        dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Chargement de la bande annonce...");
        dialog.show();
    }

    public void updateUI() {
        title.setText(displayedMovie.title);

        String extraString = "";
        extraString += "<strong>Durée</strong> : " + displayedMovie.getDuration() + "<br />";

        if (!displayedMovie.directors.equals(""))
            extraString += "<strong>Directeur</strong> : " + displayedMovie.directors + "<br />";
        if (!displayedMovie.actors.equals(""))
            extraString += "<strong>Acteurs</strong> : " + displayedMovie.actors + "<br />";
        extraString += "<strong>Genre</strong> : " + displayedMovie.genres;
        extra.setText(Html.fromHtml(extraString));
        display.setText(Html.fromHtml("<strong>" + theater + "</strong><br>" + displayedMovie.getDisplays()));
        if (displayedMovie.certificateString.equals(""))
            certificate.setVisibility(View.GONE);
        else
            certificate.setText(displayedMovie.certificateString);
        ImageLoader.getInstance().displayImage(displayedMovie.getPosterUrl(2), poster);
        poster.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallbacks.onItemSelected(-1, DetailsFragment.this, v);
            }
        });

        pressRating.setProgress(displayedMovie.getPressRating());
        if (displayedMovie.pressRating.equals("0"))
            pressRatingText.setText("");
        else if (displayedMovie.pressRating.length() > 3)
            pressRatingText.setText(displayedMovie.pressRating.substring(0, 3));
        else
            pressRatingText.setText(displayedMovie.pressRating);

        userRating.setProgress(displayedMovie.getUserRating());
        if (displayedMovie.userRating.equals("0"))
            userRatingText.setText("");
        else if (displayedMovie.userRating.length() > 3)
            userRatingText.setText(displayedMovie.userRating.substring(0, 3));
        else
            userRatingText.setText(displayedMovie.userRating);

        synopsis.setText(displayedMovie.synopsis.equals("") ? "Chargement du synopsis..." : Html.fromHtml(displayedMovie.synopsis));
        if (getActivity() != null) {
            getActivity().setTitle(displayedMovie.title);
        } else {
            titleToSet = true;
        }
    }

    @Override
    public void updateListView(ArrayList<Movie> movies) {
        // TODO Auto-generated method stub
    }

    @Override
    public void finishNoNetwork() {
        mCallbacks.finishNoNetwork();
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            int idItem = getArguments().getInt(ARG_ITEM_ID);
            displayedMovie = MoviesFragment.getMovies().get(idItem);
        }
        if (getArguments().containsKey(ARG_THEATER_NAME)) {
            theater = getArguments().getString(ARG_THEATER_NAME);
        }
    }

    private class LoadMovieTask extends AsyncTask<String, Void, Movie> {
        private SharedPreferences preferences;
        private Context ctx;

        public LoadMovieTask(DetailsFragment fragment) {
            super();
            this.ctx = fragment.getActivity();
            this.preferences = ctx.getSharedPreferences("synopsis", Context.MODE_PRIVATE);
            mCallbacks.setIsLoading(true);
        }

        @Override
        @SuppressLint("NewApi")
        protected Movie doInBackground(String... queries) {
            // Try to read synopsis from cache
            String movieCode = displayedMovie.code;
            String cache = preferences.getString(movieCode, "");
            if (!cache.equals("")) {
                Log.i("cache-hit", "Getting synopsis from cache for " + movieCode);
                displayedMovie.synopsis = cache;
            } else {
                Log.i("cache-miss", "Remote loading synopsis for " + movieCode);
                displayedMovie = (new APIHelper()).findMovie(displayedMovie);
                String synopsis = displayedMovie.synopsis;
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString(movieCode, synopsis);
                ed.apply();
                new BackupManager(ctx).dataChanged();
            }

            return displayedMovie;
        }

        @Override
        protected void onPostExecute(Movie resultsList) {
            mCallbacks.setIsLoading(false);
            displayedMovie = resultsList;
            updateUI();
        }
    }
}
