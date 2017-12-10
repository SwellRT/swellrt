package org.swellrt.beta.client.platform.java;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.TimerService;

public class JavaTimerService implements TimerService {

  public class WrappedTask extends TimerTask {

    private final Scheduler.Task task;

    public WrappedTask(Scheduler.Task task) {
      this.task = task;
    }

    @Override
    public void run() {

      if (!scheduledTasks.contains(task)) {
        return;
      }

      this.task.execute();
      scheduledTasks.remove(task);
    }

  }

  public class IncrementalWrappedTask extends TimerTask {

    private final Scheduler.IncrementalTask task;

    private int delay;
    private int interval;

    public IncrementalWrappedTask(Scheduler.IncrementalTask task) {
      this.task = task;
      this.delay = -1;
      this.interval = -1;
    }

    public IncrementalWrappedTask(Scheduler.IncrementalTask task, int delay) {
      this.task = task;
      this.delay = delay;
      this.interval = -1;
    }

    public IncrementalWrappedTask(Scheduler.IncrementalTask task, int delay,
        int interval) {
      this.task = task;
      this.delay = delay;
      this.interval = interval;
    }


    @Override
    public void run() {

      if (!scheduledTasks.contains(task)) {
        return;
      }

      if (this.task.execute()) {
        if (delay < 0 && interval < 0) {
          timer.schedule(this, 0);
        } else if (interval < 0) {
          timer.schedule(this, delay);
        } else {
          timer.schedule(this, delay, interval);
        }
      } else {
        scheduledTasks.remove(task);
      }
    }

  }


  private final Timer timer = new Timer("wave-timer-service");
  private long startTime = System.currentTimeMillis();
  private Set<Scheduler.Schedulable> scheduledTasks = new HashSet<Scheduler.Schedulable>();


  @Override
  public void schedule(Task task) {
    scheduledTasks.add(task);
    timer.schedule(new WrappedTask(task), 0);
  }

  @Override
  public void schedule(IncrementalTask task) {
    scheduledTasks.add(task);
    timer.schedule(new IncrementalWrappedTask(task), 0);
  }

  @Override
  public void scheduleDelayed(Task task, int minimumTime) {
    scheduledTasks.add(task);
    timer.schedule(new WrappedTask(task), minimumTime);
  }

  @Override
  public void scheduleDelayed(IncrementalTask task, int minimumTime) {
    scheduledTasks.add(task);
    timer.schedule(new IncrementalWrappedTask(task), minimumTime);
  }

  @Override
  public void scheduleRepeating(IncrementalTask task, int minimumTime, int interval) {
    scheduledTasks.add(task);
    timer.schedule(new IncrementalWrappedTask(task), minimumTime, interval);
  }

  @Override
  public void cancel(Schedulable job) {
    scheduledTasks.remove(job);
  }

  @Override
  public boolean isScheduled(Schedulable job) {
    return scheduledTasks.contains(job);
  }

  @Override
  public int elapsedMillis() {
    return (int) (System.currentTimeMillis() - startTime);
  }

  @Override
  public double currentTimeMillis() {
    return System.currentTimeMillis();
  }

}
