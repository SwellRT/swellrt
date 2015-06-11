package org.swellrt.android.service;

import org.swellrt.android.service.SwellRTService.SwellRTServiceCallback;
import org.swellrt.model.generic.Model;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * An extended activity integrated with a SwellRT service
 * 
 * @author Pablo Ojanguren (pablojan@gmail.com)
 * 
 */
public abstract class SwellRTActivity extends Activity implements ServiceConnection,
    SwellRTServiceCallback {

  /** The SwellRT Service reference */
  private SwellRTService mService;

  //
  // Activity lifecycle
  //

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bindService();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unbindService(this);
  }

  //
  // SwellRT service mgmt
  //

  /**
   * Bind this activity to the SwellRT service
   */
  private void bindService() {
    if (mService == null) {
      final Intent mWaveServiceIntent = new Intent(this, SwellRTService.class);
      bindService(mWaveServiceIntent, this, Context.BIND_AUTO_CREATE);
    }
  }

  protected SwellRTService getService() {
    return mService;
  }

  protected boolean isServiceReady() {
    return mService != null;
  }

  @Override
  public void onStartSessionSuccess(String session) {

  }

  @Override
  public void onStartSessionFail(String error) {

  }

  @Override
  public void onCreate(Model model) {

  }

  @Override
  public void onOpen(Model model) {

  }

  @Override
  public void onClose(boolean everythingCommitted) {

  }

  @Override
  public void onUpdate(int inFlightSize, int notAckedSize, int unCommitedSize) {

  }

  @Override
  public void onError(String message) {

  }

  @Override
  public void onDebugInfo(String message) {

  }

  @Override
  public final void onServiceConnected(ComponentName name, IBinder serviceBinder) {
    mService = ((SwellRTService.SwellRTBinder) serviceBinder).getService(this);
    onConnect();
  }

  /**
   * Notify that service is ready to use
   */
  public abstract void onConnect();

  @Override
  public final void onServiceDisconnected(ComponentName name) {
    mService = null;
    onDisconnect();
  }

  /**
   * Notify that service is no longer ready
   */
  public abstract void onDisconnect();

}
