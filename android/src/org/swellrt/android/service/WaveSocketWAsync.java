package org.swellrt.android.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereRequest.AtmosphereRequestBuilder;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.providers.grizzly.GrizzlyAsyncHttpProvider;

public class WaveSocketWAsync implements WaveSocket {

  public final static int EVENT_ON_OPEN = 1;
  public final static int EVENT_ON_CLOSE = 2;
  public final static int EVENT_ON_MESSAGE = 3;
  public final static int EVENT_ON_EXCEPTION = 4;
  public final static int EVENT_ON_CLOSE_ERROR = 5;

  // A gateway between socket thread and UI thread
  private Handler uiHandler = new Handler(Looper.getMainLooper()) {

    @Override
    public void handleMessage(android.os.Message msg) {


      switch (msg.arg1) {

      case EVENT_ON_OPEN:
        callback.onConnect();
        break;
      case EVENT_ON_CLOSE:
        callback.onDisconnect();
        break;
      case EVENT_ON_MESSAGE:

        String str = (String) msg.obj;

        if (str.indexOf('|') == 0) {

          while (str.indexOf('|') == 0 && str.length() > 1) {

            str = str.substring(1);
            int marker = str.indexOf("}|");
            callback.onMessage(str.substring(0, marker + 1));
            str = str.substring(marker + 1);

          }

        } else {
          callback.onMessage(str);
        }

        break;
      case EVENT_ON_EXCEPTION:
        callback.onDisconnect();
        break;
      case EVENT_ON_CLOSE_ERROR:
        callback.onDisconnect();
        break;

      }

    };

  };

  private final String urlBase;
  private Socket socket = null;
  private final WaveSocketCallback callback;
  private String sessionId;


  private class WebSocketRunnable implements Runnable {


    final String urlBase;
    final Handler uiHandler;
    final String sessionId;

    public WebSocketRunnable(String urlBase, Handler uiHandler, String sessionId) {
      this.urlBase = urlBase;
      this.uiHandler = uiHandler;
      this.sessionId = sessionId;
    }

    @Override
    public void run() {



      /*
       * Configure the Grizzly provider in the Async Http Client: <a href=
       * 'http://github.com/Atmosphere/wasync/wiki/Configuring-the-underlying-AHC-provider'>configure
       * AHC</a>
       */


      AsyncHttpClientConfig ahcConfig = new AsyncHttpClientConfig.Builder().build();
      AsyncHttpClient ahc = new AsyncHttpClient(new GrizzlyAsyncHttpProvider(ahcConfig));
      AtmosphereClient client = ClientFactory.getDefault().newClient(AtmosphereClient.class);


      client.newOptionsBuilder().waitBeforeUnlocking(0);
      AtmosphereRequestBuilder requestBuilder = client.newRequestBuilder()
          .method(Request.METHOD.GET).trackMessageLength(false).uri(WaveSocketWAsync.this.urlBase)
          // UrlBase
          // .transport(Request.TRANSPORT.WEBSOCKET)
          .transport(Request.TRANSPORT.LONG_POLLING)
          .header("Cookie", "WSESSIONID=" + sessionId);

      // Using waitBeforeUnlocking(2000) option to avoid high delay on long-polling connection
      WaveSocketWAsync.this.socket = client
          .create(client.newOptionsBuilder().waitBeforeUnlocking(2000).runtime(ahc).build())
          .on(Event.OPEN.name(), new Function<String>() {

            @Override
            public void on(String arg0) {

              Message msg = uiHandler.obtainMessage();
              msg.arg1 = EVENT_ON_OPEN;
              uiHandler.sendMessage(msg);

            }

          }).on(Event.CLOSE.name(), new Function<String>() {

            @Override
            public void on(String arg0) {

              Message msg = uiHandler.obtainMessage();
              msg.arg1 = EVENT_ON_CLOSE;
              uiHandler.sendMessage(msg);

            }

          }).on(Event.REOPENED.name(), new Function<String>() {

            @Override
            public void on(String arg0) {

              Message msg = uiHandler.obtainMessage();
              msg.arg1 = EVENT_ON_OPEN;
              uiHandler.sendMessage(msg);

            }

          }).on(Event.MESSAGE.name(), new Function<String>() {

            @Override
            public void on(String arg) {

              Message msg = uiHandler.obtainMessage();
              msg.arg1 = EVENT_ON_MESSAGE;
              msg.obj = arg;
              uiHandler.sendMessage(msg);

            }

          }).on(new Function<Throwable>() {

            @Override
            public void on(Throwable t) {

              Message msg = uiHandler.obtainMessage();
              msg.arg1 = EVENT_ON_EXCEPTION;
              msg.obj = t;
              uiHandler.sendMessage(msg);
            }

          });

      try {

        WaveSocketWAsync.this.socket.open(requestBuilder.build(), 5000, TimeUnit.MILLISECONDS);


      } catch (IOException e) {

        WaveSocketWAsync.this.socket = null;

        Message msg = uiHandler.obtainMessage();
        msg.arg1 = EVENT_ON_CLOSE_ERROR;
        msg.obj = e;
        uiHandler.sendMessage(msg);

      }
    }




  } // Runnable


  public WaveSocketWAsync(final WaveSocket.WaveSocketCallback callback, String urlBase,
      String sessionId) {
    this.urlBase = urlBase;
    this.callback = callback;
    this.sessionId = sessionId;
  }

  @Override
  public void connect() {

    if (socket != null) {

    }

    new Thread(new WebSocketRunnable(urlBase, uiHandler, sessionId)).start();

  }

  @Override
  public void disconnect() {

    new Thread(new Runnable() {

      @Override
      public void run() {

        WaveSocketWAsync.this.socket.close();

      }

    }).start();

  }



  @Override
  public void sendMessage(final String message) {

    new Thread(new Runnable() {

      @Override
      public void run() {

        try {

          socket.fire(message);

        } catch (IOException e) {

          Message msg = uiHandler.obtainMessage();
          msg.arg1 = EVENT_ON_EXCEPTION;
          msg.obj = e;
          uiHandler.sendMessage(msg);

        }

      }

    }).start();

  }

}

