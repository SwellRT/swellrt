package org.swellrt.beta.client;

import org.swellrt.beta.client.rest.ServiceOperation.Callback;
import org.swellrt.beta.client.rest.operations.CloseOperation;
import org.swellrt.beta.client.rest.operations.CreateUserOperation;
import org.swellrt.beta.client.rest.operations.EditUserOperation;
import org.swellrt.beta.client.rest.operations.GetUserBatchOperation;
import org.swellrt.beta.client.rest.operations.GetUserOperation;
import org.swellrt.beta.client.rest.operations.ListLoginOperation;
import org.swellrt.beta.client.rest.operations.LoginOperation;
import org.swellrt.beta.client.rest.operations.LogoutOperation;
import org.swellrt.beta.client.rest.operations.OpenOperation;
import org.swellrt.beta.client.rest.operations.PasswordOperation;
import org.swellrt.beta.client.rest.operations.PasswordRecoverOperation;
import org.swellrt.beta.client.rest.operations.QueryOperation;
import org.swellrt.beta.client.rest.operations.ResumeOperation;
import org.swellrt.beta.client.rest.operations.naming.DeleteNameOperation;
import org.swellrt.beta.client.rest.operations.naming.GetNamesOperation;
import org.swellrt.beta.client.rest.operations.naming.SetNameOperation;
import org.swellrt.beta.model.SObject;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ServiceFrontend")
public interface ServiceFrontend extends ServiceConnection {

  void createUser(CreateUserOperation.Options options,
      Callback<CreateUserOperation.Response> callback);

  void login(LoginOperation.Options options, Callback<LoginOperation.Response> callback);

  void logout(LogoutOperation.Options options, Callback<LogoutOperation.Response> callback);

  void resume(ResumeOperation.Options options, Callback<ResumeOperation.Response> callback);

  void open(OpenOperation.Options options, Callback<SObject> callback);

  void close(CloseOperation.Options options, Callback<CloseOperation.Response> callback);

  void query(QueryOperation.Options options, Callback<QueryOperation.Response> callback);

  void getUser(GetUserOperation.Options options, Callback<GetUserOperation.Response> callback);

  void getUserBatch(GetUserBatchOperation.Options options,
      Callback<GetUserBatchOperation.Response> callback);

  void editUser(EditUserOperation.Options options, Callback<EditUserOperation.Response> callback);

  void listLogin(ListLoginOperation.Options options,
      Callback<ListLoginOperation.Response> callback);

  void recoverPassword(PasswordRecoverOperation.Options options,
      Callback<PasswordRecoverOperation.Response> callback);

  void password(PasswordOperation.Options options, Callback<PasswordOperation.Response> callback);

  void getObjectNames(GetNamesOperation.Options options,
      Callback<GetNamesOperation.Response> callback);

  void setObjectName(SetNameOperation.Options options,
      Callback<SetNameOperation.Response> callback);

  void deleteObjectName(DeleteNameOperation.Options options,
      Callback<DeleteNameOperation.Response> callback);


  String getAppDomain();

}