package org.swellrt.beta.client;

import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.GetUserBatchOperation;
import org.swellrt.beta.client.rest.operations.ListLoginOperation;
import org.swellrt.beta.client.rest.operations.QueryOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.Credential;
import org.swellrt.beta.client.rest.operations.params.CredentialData;
import org.swellrt.beta.client.rest.operations.params.ObjectId;
import org.swellrt.beta.client.rest.operations.params.ObjectName;
import org.swellrt.beta.client.rest.operations.params.Void;
import org.swellrt.beta.model.SObject;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ServiceFrontend")
public interface ServiceFrontend extends ServiceConnection {

  void createUser(Account options, Callback<Account> callback);

  void login(Credential options, Callback<Account> callback);

  void logout(Credential options, Callback<Void> callback);

  void resume(Credential options, Callback<Account> callback);

  void open(ObjectId options, Callback<SObject> callback);

  void close(ObjectId options, Callback<Void> callback);

  void query(QueryOperation.Options options, Callback<QueryOperation.Response> callback);

  void getUser(Credential options, Callback<Account> callback);

  void getUserBatch(GetUserBatchOperation.Options options,
      Callback<GetUserBatchOperation.Response> callback);

  void editUser(Account options, Callback<Account> callback);

  void listLogin(ListLoginOperation.Options options,
      Callback<ListLoginOperation.Response> callback);

  void recoverPassword(CredentialData options,
      Callback<Void> callback);

  void password(CredentialData options, Callback<Void> callback);

  void getObjectNames(ObjectName options,
      Callback<Void> callback);

  void setObjectName(ObjectName options,
      Callback<Void> callback);

  void deleteObjectName(ObjectName options,
      Callback<Void> callback);


  String getAppDomain();

}