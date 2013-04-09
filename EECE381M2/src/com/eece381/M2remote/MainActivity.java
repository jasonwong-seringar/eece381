package com.eece381.M2remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.example.iptest.R;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class MainActivity extends ListActivity implements OnGestureListener, OnSeekBarChangeListener{
	private GestureDetector gestureScanner;
	private boolean isPlayerActive = false;
	private int activeScreen = 1;
	private String tag = "DEBUG";
	int songProgress = 0;
	boolean isPlaying=false;
	boolean isPaused=true;
	boolean pausable=false;
	boolean syncRequested = false;
	boolean syncActive = false;
	String readbuffer;
	String curSong;
	String curArtist;
	private static ArrayList<SongInfo> mListContent= new ArrayList<SongInfo>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// This call will result in better error messages if you
		// try to do things in the wrong thread.
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectDiskReads().detectDiskWrites().detectNetwork()
		.penaltyLog().build());
		super.onCreate(savedInstanceState);
		
		
		
		gestureScanner=new GestureDetector(this,this);
		setContentView(R.layout.activity_main);

		// Set up a timer task.  We will use the timer to check the
		// input queue every 500 ms
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 500);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	//Sends new position to Middleman
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser)
	{
		// change progress text label with current seekbar value
		char prog = (char) progress;
		TextView textProgress = (TextView) findViewById(R.id.seekValue);
		textProgress.setText("" + progress + "%");
	}

	//DEBUG for seekbar
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub	
	}

	//DEBUG for seekbar
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		//Shows where the position was originally

		Integer progress = seekBar.getProgress();
		char prog = (char)progress.intValue();
		Log.i(tag,progress.toString());
		String prog_change = "J" + prog;
		send(prog_change);   	
	}

	// Route called when the user presses "connect"
	public void openSocket(View view) {
		MyApplication app = (MyApplication) getApplication();
		TextView msgbox = (TextView) findViewById(R.id.error_message_box);

		// Make sure the socket is not already opened 

		if (app.sock != null && app.sock.isConnected() && !app.sock.isClosed()) {
			msgbox.setText("Socket already open");
			return;
		}

		// open the socket.  SocketConnect is a new subclass
		// (defined below).  This creates an instance of the subclass
		// and executes the code in it.

		new SocketConnect().execute((Void) null);

		runOnUiThread(new Runnable(){
			public void run(){

			}
		});

	}

	//  Called when the user wants to send a message

	public void sendMessage(View view) {

		// Get the message from the box

		EditText et = (EditText) findViewById(R.id.MessageText);
		String msg = et.getText().toString();
		send(msg);
	}

	private void send(String msg) {
		MyApplication app = (MyApplication) getApplication();

		// Create an array of bytes.  First byte will be the
		// message length, and the next ones will be the message

		byte buf[] = new byte[msg.length() + 1];
		buf[0] = (byte) msg.length();
		System.arraycopy(msg.getBytes(), 0, buf, 1, msg.length());

		// Now send through the output stream of the socket

		OutputStream out;
		try {
			out = app.sock.getOutputStream();
			try {
				out.write(buf, 0, msg.length() + 1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Called when the user closes a socket

	public void closeSocket(View view) {
		MyApplication app = (MyApplication) getApplication();
		Socket s = app.sock;
		try {
			s.getOutputStream().close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Construct an IP address from the four boxes

	public String getConnectToIP() {
		String addr = "";
		EditText text_ip;
		text_ip = (EditText) findViewById(R.id.ip1);
		addr += text_ip.getText().toString();
		text_ip = (EditText) findViewById(R.id.ip2);
		addr += "." + text_ip.getText().toString();
		text_ip = (EditText) findViewById(R.id.ip3);
		addr += "." + text_ip.getText().toString();
		text_ip = (EditText) findViewById(R.id.ip4);
		addr += "." + text_ip.getText().toString();
		return addr;
	}

	// Gets the Port from the appropriate field.

	public Integer getConnectToPort() {
		Integer port;
		EditText text_port;

		text_port = (EditText) findViewById(R.id.port);
		port = Integer.parseInt(text_port.getText().toString());

		return port;
	}


	// This is the Socket Connect asynchronous thread.  Opening a socket
	// has to be done in an Asynchronous thread in Android.  Be sure you
	// have done the Asynchronous Tread tutorial before trying to understand
	// this code.

	public class SocketConnect extends AsyncTask<Void, Void, Socket> {

		// The main parcel of work for this thread.  Opens a socket
		// to connect to the specified IP.

		protected Socket doInBackground(Void... voids) {
			Socket s = null;
			String ip = getConnectToIP();
			Integer port = getConnectToPort();

			try {
				s = new Socket(ip, port);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return s;
		}

		// After executing the doInBackground method, this is 
		// automatically called, in the UI (main) thread to store
		// the socket in this app's persistent storage

		protected void onPostExecute(Socket s) {
			MyApplication myApp = (MyApplication) MainActivity.this
					.getApplication();
			myApp.sock = s;
		}
	}

	// This is a timer Task.  Be sure to work through the tutorials
	// on Timer Tasks before trying to understand this code.

	public class TCPReadTimerTask extends TimerTask {
		public void run() {
			MyApplication app = (MyApplication) getApplication();
			if (app.sock != null && app.sock.isConnected()
					&& !app.sock.isClosed()) {

				try {
					InputStream in = app.sock.getInputStream();

					// See if any bytes are available from the Middleman

					int bytes_avail = in.available();
					if (bytes_avail > 0) {

						// If so, read them in and create a string

						//debounce to avoid intercepted comm 50ms
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
						}

						byte buf[] = new byte[bytes_avail];
						in.read(buf);

						final String s = new String(buf, 0, bytes_avail, "US-ASCII");
						Log.i(tag,"Received "+s+" from middleman");

						if(syncRequested)
						{
							if(!syncActive && s.contains("Y"))
							{
								Log.i(tag,"START SYNC");
								// Reset buffer and prepare for sync input
								readbuffer = s.substring(s.indexOf('Y'));
								syncActive = true;
								
								runOnUiThread(new Runnable() {
									public void run() {
										mListContent=new ArrayList<SongInfo>();
									}});

							}
							if(syncActive)
							{
								// Continue to append to buffer
								readbuffer+=s;
								//Debug output
								//Log.i(tag,"SYNCUPDATEreadbuffer:"+readbuffer);
							}

							if(readbuffer != null && readbuffer.contains("*"))
								// readbuffer is a finished string ready for parsing
							{
								readbuffer = readbuffer.substring(readbuffer.indexOf('Y')+2, readbuffer.indexOf('*'));
								Log.i(tag,"ATTEMPTING TO PARSE"+readbuffer);

								while(readbuffer.length()>1)
								{
									//Relevant indices for first song buffer
									final int index = readbuffer.charAt(1);
									int first = readbuffer.indexOf('@');
									int second = readbuffer.indexOf('@',first+1);
									int end = readbuffer.indexOf('$');
									
									//Extracting fields
									final String Title = readbuffer.substring(first+1,second);
									final String Artist = readbuffer.substring(second+1,end);
									Log.i(tag,Title+Artist+"Adding");

									//Inserting placeholder object into remembered list
									runOnUiThread(new Runnable() {
										public void run() {
											mListContent.add(new SongInfo(index,Title,Artist));
										}});

									readbuffer = readbuffer.substring(end+1);
									Log.i(tag,"Reduced readbuffer:"+readbuffer);
								}
								syncActive = false;
								syncRequested = false;
								Log.i(tag,"Leaving parse-in");

								getListView().postInvalidate();
							}
						}
						
						else
						{

							char echo = s.charAt(0);

							switch(echo)
							{
							case 'P':
								//Song currently playing
								runOnUiThread(new Runnable() {
									public void run() {
										// TODO check for progress value, and update progress bar
										pausable=true;
										isPlaying=true;
										isPaused=false;

										int start_index, song_index, end_index;
										if(s.length() >= 4)
										{
											if(s.contains("}"))
											{
												//Song information update
												//find and set artist and song name
												start_index = s.indexOf("}");
												song_index = s.substring(start_index+1,s.length()).indexOf("}")+start_index+1;
												end_index = s.substring(song_index+1,s.length()).indexOf("}")+song_index+1;
												Log.i(tag,"start"+start_index+"song"+song_index+"end"+end_index);
												if(song_index>start_index)
													curSong = s.substring(start_index+1, song_index);
												if(end_index>song_index)
													curArtist = s.substring(song_index+1, end_index);

												Log.i(tag,"\nFound songname"+curSong);
												Log.i(tag,"\nFound artist"+curArtist);

												// Reset song progress
												songProgress = 0;
												if(isPlayerActive)
												{
													TextView song_name = (TextView) findViewById(R.id.song_name);
													TextView artist = (TextView) findViewById(R.id.artist);
													ProgressBar prog_bar = (ProgressBar) findViewById(R.id.seekBar1);
													prog_bar.setProgress(0);
													song_name.setText(curSong);
													artist.setText(curArtist);

													song_name.invalidate();
													artist.invalidate();
													prog_bar.invalidate();
												}
											}
										}
										// Song Progress
										else if(s.length()>1)
										{
											char progress = s.charAt(1);
											if(0<progress && progress < 100)
											{
												songProgress = progress;
												if(isPlayerActive)
												{
													SeekBar prog_bar = (SeekBar) findViewById(R.id.seekBar1);
													prog_bar.setProgress((int)progress);

													prog_bar.invalidate();
												}
											}
										}
										if(isPlayerActive)
										{
											ImageButton stop = (ImageButton) findViewById(R.id.stop);
											stop.setImageResource(R.drawable.ic_stop);
											ImageButton play = (ImageButton) findViewById(R.id.play);
											play.setImageResource(R.drawable.ic_pause);

											stop.invalidate();
											play.invalidate();
										}
									}});
								break;

							case 'A':
								//Pause
								runOnUiThread(new Runnable() {
									public void run() {
										pausable=false;
										isPaused=true;
										if(isPlayerActive)
										{
											ImageButton play = (ImageButton) findViewById(R.id.play);
											play.setImageResource(R.drawable.ic_play);

											play.invalidate();
										}
									}});
								break;

							case 'S':
								//Stop
								runOnUiThread(new Runnable() {
									public void run() {
										// fade stop button and set play button
										isPlaying=false;
										isPaused=false;
										pausable=false;
										songProgress = 0;
										if(isPlayerActive)
										{
											ImageButton stop = (ImageButton) findViewById(R.id.stop);
											stop.setImageResource(R.drawable.ic_stop_faded);
											ImageButton play = (ImageButton) findViewById(R.id.play);
											play.setImageResource(R.drawable.ic_play);
											SeekBar prog_bar = (SeekBar) findViewById(R.id.seekBar1);
											prog_bar.setProgress(0);

											stop.invalidate();
											play.invalidate();
											prog_bar.invalidate();
										}
									}});
								break;

							case 'M':
								//Shuffle
								runOnUiThread(new Runnable() {
									public void run() {
										int shuffle_index = s.charAt(1);
										if(isPlayerActive){
											ImageButton shuff = (ImageButton) findViewById(R.id.shuffle);
											if(shuffle_index == 2){
												shuff.setImageResource(R.drawable.ic_shuffle_on);
											}
											else if(shuffle_index == 1)
											{
												shuff.setImageResource(R.drawable.ic_shuffle_off);
											}
											shuff.invalidate();
										}
									}});
								break;

							case 'R':
								//Repeat
								runOnUiThread(new Runnable() {
									public void run() {
										int repeat_index = s.charAt(1);
										ImageButton rep = (ImageButton) findViewById(R.id.repeat);
										if(isPlayerActive){
											if(repeat_index == 1){
												rep.setImageResource(R.drawable.ic_repeat_off);
											}
											else if(repeat_index == 2){
												rep.setImageResource(R.drawable.ic_repeat_song);
											}
											rep.invalidate();
										}
									}});
								break;

							case 'E':
								//Error from DE2
								/*runOnUiThread(new Runnable() {
								public void run() {

								}});*/
								break;

							default:
								Log.i(tag,"ERROR IN RECEIVED MESSAGE");
								break;
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Drag and drop listeners-------------------------------------------------
	private DropListener mDropListener = 
			new DropListener() {
		public void onDrop(int from, int to) {
			ListAdapter adapter = getListAdapter();
			if (adapter instanceof DragNDropAdapter) {
				((DragNDropAdapter)adapter).onDrop(from, to);
				getListView().invalidateViews();
			}
		}
	};

	private RemoveListener mRemoveListener =
			new RemoveListener() {
		public void onRemove(int which) {
			ListAdapter adapter = getListAdapter();
			if (adapter instanceof DragNDropAdapter) {
				((DragNDropAdapter)adapter).onRemove(which);
				getListView().invalidateViews();
			}
		}
	};

	private DragListener mDragListener =
			new DragListener() {

		int backgroundColor = 0xe0103010;
		int defaultBackgroundColor;

		public void onDrag(int x, int y, ListView listView) {
			// TODO Auto-generated method stub
		}

		public void onStartDrag(View itemView) {
			itemView.setVisibility(View.INVISIBLE);
			defaultBackgroundColor = itemView.getDrawingCacheBackgroundColor();
			itemView.setBackgroundColor(backgroundColor);
			ImageView iv = (ImageView)itemView.findViewById(R.id.ImageView01);
			if (iv != null) iv.setVisibility(View.INVISIBLE);
		}

		public void onStopDrag(View itemView) {
			itemView.setVisibility(View.VISIBLE);
			itemView.setBackgroundColor(defaultBackgroundColor);
			ImageView iv = (ImageView)itemView.findViewById(R.id.ImageView01);
			if (iv != null) iv.setVisibility(View.VISIBLE);
		}

	};

	//Gesture recognition------------------------------------------------------
	@Override
	public boolean onTouchEvent(MotionEvent me)
	{
		return gestureScanner.onTouchEvent(me);
	}
	@Override
	public boolean onDown(MotionEvent arg0) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		if(isPlayerActive){
			isPlayerActive = false;

			if(velocityX>0){
				activeScreen=3;

				setContentView(R.layout.main_playlist);

				if (mListContent != null)
				{
					ArrayList<String> content = new ArrayList<String>(mListContent.size());
					for (int i=0; i < mListContent.size(); i++) {
						// display?
						content.add(mListContent.get(i).getTitle());
					}

					setListAdapter(new DragNDropAdapter(this, new int[]{R.layout.dragitem}, new int[]{R.id.TextView01}, content));
					ListView listView = getListView();

					if (listView instanceof DragNDropListView) {
						((DragNDropListView) listView).setDropListener(mDropListener);
						((DragNDropListView) listView).setRemoveListener(mRemoveListener);
						((DragNDropListView) listView).setDragListener(mDragListener);
					}
				}

			}else if(velocityX<0){
				activeScreen=1;

				setContentView(R.layout.activity_main);
			}
		}
		else{
			if((activeScreen == 1 && velocityX>0) ||(activeScreen == 3 && velocityX<0))
			{
				isPlayerActive = true;
				activeScreen = 2;
				setContentView(R.layout.activity_player);

				SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
				seek.setOnSeekBarChangeListener(this);

				ImageButton play = (ImageButton) findViewById(R.id.play);
				ImageButton stop = (ImageButton) findViewById(R.id.stop);
				SeekBar prog_bar = (SeekBar) findViewById(R.id.seekBar1);
				prog_bar.setProgress(songProgress);
				if(isPlaying)
				{

					TextView song_name = (TextView) findViewById(R.id.song_name);
					TextView artist = (TextView) findViewById(R.id.artist);
					song_name.setText(curSong);
					artist.setText(curArtist);
					stop.setImageResource(R.drawable.ic_stop);

					if(pausable)
						play.setImageResource(R.drawable.ic_pause);
					else
						play.setImageResource(R.drawable.ic_play);
				}
				else
				{
					play.setImageResource(R.drawable.ic_play);
					stop.setImageResource(R.drawable.ic_stop_faded);
				}
			}
		}

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	// Android interface methods-----------------------------------------------
	public void onPlay(View view) {
		if(isPlaying)
		{
			send("P");
		}
		else
		{
			send("G");
		}
	}
	public void onStop(View view) {
		send("S");
	}

	public void onFwd(View view) {
		send("N");
	}
	public void onBack(View view) {
		send("B");
	}
	public void volume_up(View view){
		send("VU");
	}
	public void volume_down(View view){
		send("VD");
	}
	public void onRepeat(View view){
		send("R");
	}
	public void onShuffle(View view){
		send("M");
	}
	public void onSync(View view){
		send("Y");
		syncRequested = true;
		syncActive = false;
	}

	public void sendPlaylist(View view){
		ListView listView = getListView();
		String toSend = "X";
		ArrayList<SongInfo> newListContent = new ArrayList<SongInfo>();
		for(int i = 0; i<listView.getCount();i++)
		{
			String find = (String) listView.getItemAtPosition(i);
			for(int j=0; j<mListContent.size();j++)
			{
				if(mListContent.get(j).getTitle().equals(find))
				{
					//Send ordered indices
					toSend+=(char)mListContent.get(j).getIndex();
					//Remember the new list order
					newListContent.add(mListContent.get(j));
				}
			}
		}
		mListContent = newListContent;
		// Send full command
		send(toSend);
	}
}
