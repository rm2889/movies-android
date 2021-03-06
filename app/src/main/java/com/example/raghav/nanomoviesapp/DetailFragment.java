package com.example.raghav.nanomoviesapp;


import android.app.Fragment;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.raghav.nanomoviesapp.data.MovieContract;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class DetailFragment extends Fragment {

    private MovieData mCurrentMovie;
    private Context mContext;

    LinearLayout mCurrentLayout;
    private int mSavedPosition = -1;

    public final static String MOVIE_TAG = "MOVIE_TAG";
    public final static String SCROLL_TAG = "SCROLL_TAG";

    public DetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mCurrentLayout = (LinearLayout) rootView.findViewById(R.id.main_layout);

        if (savedInstanceState != null) {
            mCurrentMovie = savedInstanceState.getParcelable(MOVIE_TAG);
            mSavedPosition = savedInstanceState.getInt(SCROLL_TAG);
        }

        mContext = getActivity();
        if (getArguments() != null) {
            mCurrentMovie = getArguments().getParcelable("CURRENT_MOVIE");
            ((TextView) rootView.findViewById(R.id.movie_title_bar))
                    .setText(mCurrentMovie.getTitle());

            ImageView moviePoster = (ImageView) rootView.findViewById(R.id.detail_poster_image);
            moviePoster.setScaleType(ImageView.ScaleType.FIT_XY);

            Picasso.with(mContext)
                    .load(mCurrentMovie.getFullImageUrl())
                    .into(moviePoster);

            TextView ratingText = (TextView) rootView.findViewById(R.id.rating_text);
            ratingText.setText("Rating: " + mCurrentMovie.getVoteAverage());

            TextView releaseDateText = (TextView) rootView.findViewById(R.id.release_date_text);
            releaseDateText.setText("Release Date: " + mCurrentMovie.getReleaseDate());

            TextView synopsisText = (TextView) rootView.findViewById(R.id.synopsis_text);
            synopsisText.setText(mCurrentMovie.getOverview());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String preferenceSortBy = prefs.getString(getString(R.string.pref_sorting_key), getString(R.string.pref_sorting_default));

            if (savedInstanceState == null) {
                if (preferenceSortBy.equals(getString(R.string.pref_sorting_favorites))) {
//                get details from the database
                    fetchMovieDetailsFromDB();
                } else {
                    fetchMovieDetails();
                }
            } else {
                updateRemainingUI();
            }

            Button markFavButton = (Button) rootView.findViewById(R.id.fav_button);
            markFavButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    first check to see if it already exists in the database,
//                    if yes, display toast saying "hey already saved as a fav"
                    Cursor favCursor = mContext.getContentResolver().query(
                            MovieContract.FavoriteEntry.CONTENT_URI,
                            null,
                            MovieContract.FavoriteEntry.COLUMN_MOVIE_NAME + " = ? ",
                            new String[]{mCurrentMovie.getTitle()},
                            null
                    );

                    if (favCursor.moveToFirst()) {
                        Toast toast = Toast.makeText(mContext, "Already exists in favs!", Toast.LENGTH_SHORT);
                        toast.show();
                    }

//                  else add to database, and display toast saying "Saved as a fav!"
                    else {
                        long favoriteId;

                        ContentValues favoriteValues = new ContentValues();
                        favoriteValues.put(MovieContract.FavoriteEntry.COLUMN_DESCRIPTION,
                                mCurrentMovie.getOverview());
                        favoriteValues.put(MovieContract.FavoriteEntry.COLUMN_IMAGE_URI,
                                mCurrentMovie.getFullImageUrl());
                        favoriteValues.put(MovieContract.FavoriteEntry.COLUMN_MOVIE_NAME,
                                mCurrentMovie.getOriginalTitle());
                        favoriteValues.put(MovieContract.FavoriteEntry.COLUMN_RATING,
                                mCurrentMovie.getVoteAverage());
                        favoriteValues.put(MovieContract.FavoriteEntry.COLUMN_RELEASE_DATE,
                                mCurrentMovie.getReleaseDate());

                        Uri insertedUri = mContext.getContentResolver().insert(
                                MovieContract.FavoriteEntry.CONTENT_URI,
                                favoriteValues);

                        favoriteId = ContentUris.parseId(insertedUri);

                        for (int i = 0; i < mCurrentMovie.getReviews().size(); i++) {
                            ContentValues reviewValues = new ContentValues();
                            reviewValues.put(MovieContract.ReviewEntry.COLUMN_DESCRIPTION,
                                    mCurrentMovie.getReviews().get(i));
                            reviewValues.put(MovieContract.ReviewEntry.COLUMN_FAVORITE_ID, favoriteId);

                            Uri insertedReview = mContext.getContentResolver().insert(
                                    MovieContract.ReviewEntry.CONTENT_URI,
                                    reviewValues
                            );
                        }

                        for (final Map.Entry trailer : mCurrentMovie.getTrailers().entrySet()) {
                            ContentValues trailerValues = new ContentValues();
                            trailerValues.put(MovieContract.TrailerEntry.COLUMN_DESCRIPTION,
                                    (String) trailer.getValue());
                            trailerValues.put(MovieContract.TrailerEntry.COLUMN_FAVORITE_ID, favoriteId);
                            trailerValues.put(MovieContract.TrailerEntry.COLUMN_URI, (String) trailer.getKey());
                            Uri insertedTrailer = mContext.getContentResolver().insert(
                                    MovieContract.TrailerEntry.CONTENT_URI,
                                    trailerValues
                            );
                        }
                        Toast toast = Toast.makeText(mContext, "Added to favs!", Toast.LENGTH_SHORT);
                        toast.show();
                    }
                    favCursor.close();
                }
            });


            if (mSavedPosition != -1) {
                rootView.post(new Runnable() {
                    @Override
                    public void run() {
                        rootView.scrollTo(0, mSavedPosition);
                    }
                });
            }


        }


        return rootView;
    }

    private void fetchMovieDetailsFromDB() {
        Cursor favCursor = mContext.getContentResolver().query(
                MovieContract.FavoriteEntry.CONTENT_URI,
                null,
                MovieContract.FavoriteEntry.COLUMN_MOVIE_NAME + " = ? ",
                new String[]{mCurrentMovie.getTitle()},
                null
        );

        long favMovieId = -1;

        if (favCursor.moveToFirst()) {
            favMovieId = favCursor.getLong(favCursor.getColumnIndex("_id"));
        }
        favCursor.close();

        Cursor reviewsCursor = mContext.getContentResolver().query(
                MovieContract.ReviewEntry.CONTENT_URI,
                null,
                MovieContract.ReviewEntry.COLUMN_FAVORITE_ID + " = ? ",
                new String[]{String.valueOf(favMovieId)},
                null
        );
        if (reviewsCursor.moveToFirst()) {
            do {
                mCurrentMovie.getReviews().add(reviewsCursor.getString(reviewsCursor.getColumnIndex(MovieContract.ReviewEntry.COLUMN_DESCRIPTION)));
            } while (reviewsCursor.moveToNext());

        }
        reviewsCursor.close();

        Cursor trailersCursor = mContext.getContentResolver().query(
                MovieContract.TrailerEntry.CONTENT_URI,
                null,
                MovieContract.TrailerEntry.COLUMN_FAVORITE_ID + " = ? ",
                new String[]{String.valueOf(favMovieId)},
                null
        );
        if (trailersCursor.moveToFirst()) {
            HashMap<String, String> favTrailers = new HashMap<String, String>();
            do {
                favTrailers.put(trailersCursor.getString(trailersCursor.getColumnIndex(MovieContract.TrailerEntry.COLUMN_URI)), trailersCursor.getString(trailersCursor.getColumnIndex(MovieContract.TrailerEntry.COLUMN_DESCRIPTION)));
            } while (trailersCursor.moveToNext());

            mCurrentMovie.setTrailers(favTrailers);
        }
        trailersCursor.close();
        updateRemainingUI();

    }

    private void fetchMovieDetails() {
        RequestParams params = new RequestParams();
        params.put("api_key", Constants.API_KEY);
        params.put("append_to_response", "reviews,videos");

        MoviesdbRestClient.get("/" + String.valueOf(mCurrentMovie.getId()), true, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.v("Success Response", Integer.toString(statusCode));
                Log.v("Success Response", response.toString());
                HashMap<String, String> responseTrailers = new HashMap<String, String>();
                try {
                    JSONArray reviewArray = response.getJSONObject("reviews").getJSONArray("results");
                    for (int i = 0; i < reviewArray.length(); i++) {
                        JSONObject currentObject = reviewArray.getJSONObject(i);
                        mCurrentMovie.getReviews().add(currentObject.getString("content"));
                    }
                    JSONArray videoArray = response.getJSONObject("videos").getJSONArray("results");
                    for (int i = 0; i < videoArray.length(); i++) {
                        JSONObject currentObject = videoArray.getJSONObject(i);
                        Log.v("KEY", currentObject.getString("key"));
                        responseTrailers.put(currentObject.getString("key"),
                                currentObject.getString("name"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mCurrentMovie.setTrailers(responseTrailers);
                updateRemainingUI();

            }

            @Override
            public void onFailure(int statusCode,
                                  Header[] headers,
                                  Throwable throwable,
                                  JSONObject responseJSON) {
                Log.d("Failed Response", Integer.toString(statusCode));
            }
        });

    }

    private void updateRemainingUI() {
        HashMap<String, String> trailers = mCurrentMovie.getTrailers();
        ArrayList<String> reviews = mCurrentMovie.getReviews();
        int buttonDimension = (int) getResources().getDimension(R.dimen.trailer_size);
        int imageDimension = (int) getResources().getDimension(R.dimen.play_image_size);

        TextView trailerHeaderTextView = new TextView(mContext);
        LinearLayout.LayoutParams trailerHeaderlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        trailerHeaderTextView.setText("Trailers");
        trailerHeaderTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        trailerHeaderTextView.setLayoutParams(trailerHeaderlp);
        mCurrentLayout.addView(trailerHeaderTextView);

        for (final Map.Entry movie : trailers.entrySet()) {
            LinearLayout trailerLayout = new LinearLayout(mContext);
            trailerLayout.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            buttonDimension));
            trailerLayout.setOrientation(LinearLayout.HORIZONTAL);
            trailerLayout.setWeightSum(3f);
            mCurrentLayout.addView(trailerLayout);

            ImageButton btn = new ImageButton(mContext);
            LinearLayout.LayoutParams lpImageButton = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
            btn.setLayoutParams(lpImageButton);
            Picasso.with(mContext)
                    .load(R.drawable.play_button)
                    .resize(imageDimension, imageDimension)
                    .centerInside()
                    .into(btn);
            trailerLayout.addView(btn);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    Uri.parse("vnd.youtube:" + movie.getKey()));
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.youtube.com/watch?v=" + movie.getKey()));
                    startActivity(intent);
                }
            });

            TextView tv = new TextView(mContext);
            LinearLayout.LayoutParams lpTextView = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 2.0f);
            tv.setText((String) movie.getValue());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setLayoutParams(lpTextView);

            trailerLayout.addView(tv);
        }

        View divider = new View(mContext);
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) getResources().getDimension(R.dimen.divider_height));
        divider.setLayoutParams(dividerLp);
        divider.setBackgroundColor(getResources().getColor(R.color.divider));
        mCurrentLayout.addView(divider);


        for (String review : reviews) {
            TextView tv = new TextView(mContext);
            tv.setText(review);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            tv.setMaxLines(3);
            tv.setEllipsize(TextUtils.TruncateAt.END);


            mCurrentLayout.addView(tv);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView tv = (TextView) v;
                    Layout layout = tv.getLayout();
                    int lines = layout.getLineCount();
                    if (lines > 2) {
                        int ellipsisCount = layout.getEllipsisCount(2);
                        if (ellipsisCount > 0) {
                            tv.setEllipsize(null);
                            tv.setMaxLines(Integer.MAX_VALUE);
                        } else {
                            tv.setMaxLines(3);
                            tv.setEllipsize(TextUtils.TruncateAt.END);
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(MOVIE_TAG, mCurrentMovie);
        ScrollView thisView = (ScrollView) mCurrentLayout.getParent();
        outState.putInt(SCROLL_TAG, thisView.getScrollY());
    }
}
