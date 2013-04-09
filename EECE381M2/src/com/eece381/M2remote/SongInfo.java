package com.eece381.M2remote;

public class SongInfo {
	private int index;
	private String title;
	private String artist;
	
	SongInfo(int i, String t, String a)
	{
		index = i;
		title = t;
		artist = a;
	}

	int getIndex()
	{
		return index;
	}

	String getTitle()
	{
		return title;
	}
	
	String getArtist()
	{
		return artist;
	}

}
