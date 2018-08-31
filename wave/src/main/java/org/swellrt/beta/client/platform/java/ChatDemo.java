package org.swellrt.beta.client.platform.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.CredentialImpl;
import org.swellrt.beta.client.rest.operations.params.ObjectIdImpl;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SObject;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ChatDemo {

  private String objectId = "local.net/chat";

  private SObject object = null;
  private SList messages = null;

  private CountDownLatch startUpLatch = new CountDownLatch(1);

  private final String serviceUrl;
  private final String user;
  private final String password;

  private SMutationHandler onMessage = new SMutationHandler() {

    @Override
    public boolean exec(SEvent e) {

      if (e.isAddEvent()) {
        JsonObject message = (JsonObject) e.getValue();
        System.out.println("");
        System.out.println("[" + message.get("author").getAsString() + "]");
        System.out.println("[" + message.get("time").getAsLong() + "]");
        System.out.println(message.get("content").getAsString());
        System.out.println("");
        System.out.print("> ");
      }

      return false;
    }

  };

  public ChatDemo(String serviceUrl, String user, String password) {
    this.serviceUrl = serviceUrl;
    this.user = user;
    this.password = password;
  }

  public void run() throws InterruptedException {

    ServiceFrontend service = Swell.getService(serviceUrl);

    service.login(new CredentialImpl(user, password, true),
        new ServiceOperation.Callback<Account>() {

          @Override
          public void onError(SException exception) {
            if (exception.getCause() != null)
              System.err.println(exception.getCause().getMessage());
            else
              System.err.println(exception.getMessage());
          }

          @Override
          public void onSuccess(Account response) {
            System.out.println("Login as " + response.getId());

            service.open(new ObjectIdImpl(objectId),
                new ServiceOperation.Callback<SObject>() {

                  @Override
                  public void onError(SException exception) {
                    System.err.println(exception.getMessage());
                  }

                  @Override
                  public void onSuccess(SObject object) {
                    System.out.println("Connected to object " + object.getId());

                    try {

                      ChatDemo.this.object = object;
                      ChatDemo.this.messages = (SList) object.node("messages");
                      ChatDemo.this.messages.listen(onMessage);

                      ChatDemo.this.startUpLatch.countDown();

                    } catch (SException e) {
                      e.printStackTrace();
                    }
                  }

                });

          }
        });

    startUpLatch.await();

    interactiveLoop();

  }

  public void interactiveLoop() {

    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

    boolean exit = false;
    do {

      System.out.print("> ");

      String in = null;
      try {
        in = r.readLine();
      } catch (IOException e) {

      }

      if (in == null) {
        exit = true;
        continue;
      }

      if (messages != null) {
        JsonObject m = new JsonObject();
        m.add("author", new JsonPrimitive("java client"));
        m.add("time", new JsonPrimitive(System.currentTimeMillis()));
        m.add("content", new JsonPrimitive(in));

        try {
          messages.add(m);
          System.out.println("(messagen sent)");
        } catch (SException e) {
          System.out.println("(error sending message: " + e.getCode() + ")");
        }
      }



    } while (!exit);

  }

  public static void main(String[] args) {

    ChatDemo chat = new ChatDemo("http://localhost:9898",
        ParticipantId.anyoneUniversal("local.net").getAddress(), "");

    try {
      chat.run();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

}
