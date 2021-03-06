package com.example.raghav.nanomoviesapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by raghav on 7/14/15.
 */

public class MovieData implements Parcelable {

    private static final String IMAGE_BASE_URL = "http://image.tmdb.org/t/p/w185/";

    private double voteAverage;
    private int id;
    private String title;
    private String overview;
    private String originalTitle;
    private String posterPath;
    private double popularity;
    private ArrayList<String> reviews;
    private HashMap<String, String> trailers;

    public HashMap<String, String> getTrailers() {
        return trailers;
    }

    public void setTrailers(HashMap<String, String> trailers) {
        this.trailers = trailers;
    }



    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    private String releaseDate;

    public MovieData(double voteAverage, int id, String title, String overview, String originalTitle, String posterPath, double popularity, String releaseDate) {
        this.voteAverage = voteAverage;
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.originalTitle = originalTitle;
        this.posterPath = posterPath;
        this.popularity = popularity;
        this.releaseDate = releaseDate;
        this.reviews = new ArrayList<>();
        this.trailers = new HashMap<>();
    }


    public ArrayList<String> getReviews() {
        return reviews;
    }

    public String getFullImageUrl() {
        return IMAGE_BASE_URL + this.posterPath;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(int voteAverage) {
        this.voteAverage = voteAverage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(float popularity) {
        this.popularity = popularity;
    }

    protected MovieData(Parcel in) {
        id = in.readInt();
        title = in.readString();
        overview = in.readString();
        originalTitle = in.readString();
        posterPath = in.readString();
        popularity = in.readDouble();
        releaseDate = in.readString();
        voteAverage = in.readDouble();
        if (in.readByte() == 0x01) {
            reviews = new ArrayList<String>();
            in.readList(reviews, String.class.getClassLoader());
        } else {
            reviews = null;
        }
        trailers = (HashMap) in.readValue(HashMap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(overview);
        dest.writeString(originalTitle);
        dest.writeString(posterPath);
        dest.writeDouble(popularity);
        dest.writeString(releaseDate);
        dest.writeDouble(voteAverage);
        if (reviews == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(reviews);
        }
        dest.writeValue(trailers);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MovieData> CREATOR = new Parcelable.Creator<MovieData>() {
        @Override
        public MovieData createFromParcel(Parcel in) {
            return new MovieData(in);
        }

        @Override
        public MovieData[] newArray(int size) {
            return new MovieData[size];
        }
    };
}
