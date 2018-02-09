package org.swellrt.sandbox.editor;

import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocHistoryFake;
import org.waveprotocol.wave.client.editor.playback.DocHistoryFakeBuilder;

public class SampleDocHistories {

  public static DocHistory getHistoryOne() {

    DocHistoryFakeBuilder db = new DocHistoryFakeBuilder();

    // revision #0

    db.appendLineAsDelta("The opening ceremony of any Olympics provides pageantry at a global scale", "ann@local.net");
    db.appendLineAsDelta("a celebration that, at its best, can create moments every bit as indelible as the games themselves", "ann@local.net");
    db.appendLineAsDelta("In Pyeongchang, the curtain-raiser also includes a site never seen before", "ann@local.net");
    db.appendLineAsDelta("a record-setting 1,218 drones joined in a mechanical murmuration.", "ann@local.net");

    // revision #1

    db.appendLineAsDelta("Drone shows like the one on display at the Pyeongchang Games have taken place before,", "bob@local.net");
    db.appendLineAsDelta("you may remember the drone army that flanked Lady Gaga at last year's Super Bowl.", "bob@local.net");
    db.appendLineAsDelta("But the burst of drones that filled the sky Friday night -or early morning", "bob@local.net");
    db.appendLineAsDelta("depending on where in the world you watched-", "bob@local.net");
    db.appendLineAsDelta("comprised four times as many fliers.", "bob@local.net");
    db.appendLineAsDelta("Without hyperbole, there's really never been anything like it.",
        "bob@local.net");

    // revision #2 ... #7

    db.appendLineAsDelta("As at the Super Bowl, the Pyeongchang drone show comes compliments of Intel's Shooting Star platform, which enables a legion of foot-long","ann@local.net");
    db.appendLineAsDelta("eight ounce, plastic and foam quadcopters to fly in sync, swooping and swirling along an animator's prescribed path","bob@local.net");
    db.appendLineAsDelta("It's in essence technology meeting art, says Anil Nanduri, general manager of Intel's drone group.","ann@local.net");
    db.appendLineAsDelta("Also like the Super Bowl, the opening ceremony production you'll see on your TV or streaming device was prerecorded","bob@local.net");
    db.appendLineAsDelta("That's less of a cheat than an insurance policy; tiny drones can only handle so much abuse","ann@local.net");
    db.appendLineAsDelta("and Pyeongchang is a cold and windy city.","bob@local.net");

    return new DocHistoryFake(db.getDeltas());

  }

}
