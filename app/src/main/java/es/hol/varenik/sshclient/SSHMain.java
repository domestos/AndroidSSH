package es.hol.varenik.sshclient;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SSHMain extends AppCompatActivity {

    private static String TAG = "sshLog";
    private static Session session;
    private static ChannelShell channelSell;
    private static List<String> commands;
    private AsyncSession a;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sshmain);

        commands = new ArrayList<String>();
        //commands.add("cat /home/varenik/Desktop/linlssh");
        commands.add("export DISPLAY=:0 ");
       // commands.add("gnome-open https:google.com");
        // commands.add("gnome-open https:vk.com");

        //session = AsyncSession.getSession();


        Button btnUrl = (Button) findViewById(R.id.btnURL);
        final EditText etUrt = (EditText) findViewById(R.id.etUrl);

        btnUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            String url  = etUrt.getText().toString();
                if (!url.isEmpty()) {
                    commands.add("gnome-open "+url);
                    new AsyncSession().execute();

                }

            }
        });


    }


    class AsyncSession extends AsyncTask<Void, String, ChannelShell> {

        private Session getSession() {
            if (session == null || !session.isConnected()) {
                Log.i(TAG, "begin creat session");
                session = connect("192.168.0.103", "varenik", "4554722");
                return session;
            }
            Log.i(TAG, "session exist");
            return session;
        }

        private Session connect(String hostname, String username, String password) {

            JSch jSch = new JSch();

            try {

                session = jSch.getSession(username, hostname, 22);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.setPassword(password);


                Log.i(TAG, "begin connect");
                session.connect();
                Log.i(TAG, "connected");
                Log.i(TAG, "status session: " + session.isConnected() + " || " + session.hashCode());
            } catch (Exception e) {
                Log.e(TAG, "An error occurred while connecting to " + hostname + ": " + e);
            }
            Log.i(TAG, "server Info: getServerVersion =" + session.getServerVersion());
            Log.i(TAG, "server Info: getServerAliveCountMax =" + session.getServerAliveCountMax());
            return session;

        }

        private Channel getChannel() throws JSchException {

            if (channelSell == null || !channelSell.isConnected()) {
                try {
                    channelSell = (ChannelShell) getSession().openChannel("shell");
                    Log.i(TAG, "begin connect channel");

                    channelSell.connect();
                    Log.i(TAG, "channel Info: getID =" + channelSell.getId());

                    Log.i(TAG, "connected channel");
                    Log.i(TAG, "status channel: " + channelSell.isConnected() + " || " + channelSell.hashCode());

                } catch (Exception e) {
                    Log.e(TAG, "Error while opening channelSell: " + e);


                }
            }
            return channelSell;
        }

        private void executeCommands(List<String> commands) {

            try {
                Channel channel = getChannel();
                Log.i(TAG, "Sending commands...");
                sendCommands(channel, commands);

                readChannelOutput(channel);
                Log.i(TAG, "Finished sending commands! ");

            } catch (Exception e) {
                Log.i(TAG, "An error ocurred during executeCommands: " + e);
            }
        }

        private void sendCommands(Channel channel, List<String> commands) {

            try {
                PrintStream out = new PrintStream(channel.getOutputStream());
                Log.i(TAG, "send command in chanel " + channel.hashCode());
                out.println("#!/bin/bash");
                for (String command : commands) {
                    out.println(command);
                }
                out.println("exit");

                out.flush();
                out.close();


            } catch (Exception e) {
                Log.e(TAG, "Error while sending commands: " + e);
            }

        }

        private void readChannelOutput(Channel channel) {

            byte[] buffer = new byte[1024];

            String line = "";
            try {
                InputStream in = channel.getInputStream();
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(buffer, 0, 1024);
                        if (i < 0) {
                            break;
                        }
                        line = new String(buffer, 0, i);
                        Log.w(TAG, " line " + line);
                    }

                    if (line.contains("logout")) {
                        break;
                    }

                    if (channel.isClosed()) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while reading channel output: " + e);
            }
        }

        public void close() {
            channelSell.disconnect();
            // session.disconnect();
            System.out.println("Disconnected channel and session");
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            Toast.makeText(SSHMain.this, "Working", Toast.LENGTH_SHORT).show();
            TextView tv = (TextView) findViewById(R.id.status);

            tv.setText("working");
            super.onProgressUpdate(values);
        }

        @Override
        protected ChannelShell doInBackground(Void... params) {
            executeCommands(commands);
            close();
            return null;
        }


    }

}
