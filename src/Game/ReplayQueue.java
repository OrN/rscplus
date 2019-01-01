/**
 * rscplus
 *
 * <p>This file is part of rscplus.
 *
 * <p>rscplus is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * <p>rscplus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public License along with rscplus. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * <p>Authors: see <https://github.com/OrN/rscplus>
 */
package Game;

import Client.Launcher;
import Client.Logger;
import Client.Settings;
import Client.Util;
import Client.QueueWindow;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ReplayQueue {
  public static int currentIndex = 0;
  public static int lastIndex = 1;
  public static String currentReplayName = "";
  public static boolean skipped = false;
  public static ArrayList<File> queue = new ArrayList<File>();
  public static boolean foundBrokenReplay = false;

  // returns if it found valid replays in the directory chosen
  public static boolean replayFileSelectAdd() {
    JFileChooser j = new JFileChooser(Settings.Dir.REPLAY);
    j.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int response = j.showDialog(Game.getInstance().getApplet(), "Select Folder");

    File selection = j.getSelectedFile();
    if (selection != null && response != JFileChooser.CANCEL_OPTION) {
      ReplayQueue.currentReplayName = selection.getPath();
      if (Replay.isValid(ReplayQueue.currentReplayName)) {
        return true;
      } else {
        List<File> selectionArr = new ArrayList<File>();
        selectionArr.add(selection);
        List<File> replays = Util.getAllReplays(selectionArr);
        if (replays.size() > 0) {
          ReplayQueue.queue.addAll(replays);
          QueueWindow.copyQueueToTable();
          Logger.Info(
              String.format(
                  "Added %d replays to the queue. New size: %d",
                  replays.size(), ReplayQueue.queue.size()));
          return true;
        } else {
          JOptionPane.showMessageDialog(
              Game.getInstance().getApplet(),
              "The replay folder you selected is not valid.\n"
                  + "\n"
                  + "You need to select a folder that contains the 'version.bin', 'in.bin.gz', and 'keys.bin' for your replay.\n"
                  + "They're usually in a folder with your login username.",
              "rscplus",
              JOptionPane.ERROR_MESSAGE,
              Launcher.icon_warn);
        }
      }
    }
    return false;
  }

  static DropTarget dropReplays = //FIXME this gets called too many times if you drag and drop multiple folders at once
      new DropTarget() {
        public synchronized void drop(DropTargetDropEvent evt) {
          try {
            evt.acceptDrop(DnDConstants.ACTION_LINK);
            List<File> droppedFiles =
                (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            List<File> replays = Util.getAllReplays(droppedFiles);

            if (replays.size() == 0) {
              // no valid replays
              if (foundBrokenReplay) {
                JOptionPane.showMessageDialog(
                    Game.getInstance().getApplet(),
                    "The replay you dragged onto the client has a keys.bin file which is empty.\n"
                        + "The data inside is encrypted without a key to decrypt it. :(\n\n"
                        + "Some information might be able to be retrieved from this replay\n"
                        + "through reverse engineering, but basically it's broken.",
                    "rscplus",
                    JOptionPane.ERROR_MESSAGE,
                    Launcher.icon_warn);
              } else {
                // nothing that even looks like a replay was found
                JOptionPane.showMessageDialog(
                    Game.getInstance().getApplet(),
                    "The folder you dropped onto the client is not a replay, nor does it contain replay folders.\n"
                        + "\n"
                        + "You need to drop a folder that contains a 'version.bin', 'in.bin.gz', and 'keys.bin' for the replay.",
                    "rscplus",
                    JOptionPane.ERROR_MESSAGE,
                    Launcher.icon_warn);
              }
              return;
            } else {
              // at least 1 replay found
              ReplayQueue.queue.addAll(replays);
              QueueWindow.copyQueueToTable();
              Logger.Info(
                  String.format(
                      "Added %d replay%s to the queue. New size: %d",
                      replays.size(), replays.size() != 1 ? "s" : "", ReplayQueue.queue.size()));

              if (Client.state == Client.STATE_LOGIN) {
                ReplayQueue.nextReplay();
              }
            }
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      };

  public static void nextReplay() {
    if (currentIndex < queue.size()) {
      lastIndex = currentIndex;
      playFromQueue(currentIndex);
      currentIndex++;
    } else {
      Logger.Info("Reached end of queue!");
    }
  }

  public static void previousReplay() {
    if (currentIndex > 1) { //TODO verify as working
      lastIndex = currentIndex;
      currentIndex--;
      playFromQueue(currentIndex - 1);
    } else {
      Logger.Info("Reached beginning of queue!");
    }
  }

  public static void skipToReplay(int index) {
    skipped = true;
    lastIndex = currentIndex;
    currentIndex = index + 1;
    playFromQueue(index);
  }
  public static void removeReplay(int index) {
    Logger.Debug(String.format("removing %d aka %s",index,queue.get(index)));
    queue.remove(index);
  }

  private static void playFromQueue(int index) {
    if (index < 0) {
      index = 0;
    }
    if (index > queue.size() - 1) {
      index = queue.size() - 1;
    }

    if (Replay.isPlaying) {
      Replay.controlPlayback("stop");
      try {
        Thread.sleep(800); // without this at all, client says user is still logged in lol
      } catch (Exception e) { // through experimentation, I found that 700 is not long enough.
        Logger.Debug(e.toString());
        // this value might work. shorter, and the replay server has trouble keeping its
      } // timestamps straight... TODO: eliminate need for this delay.
    }

    currentReplayName = queue.get(index).getAbsolutePath();
    Logger.Info("Selected (" + index + "): " + currentReplayName);
    Client.runReplayHook = true;
    QueueWindow.updatePlaying();
  }

  public static void clearQueue() {
    queue = new ArrayList<File>();
    lastIndex = -1;
    currentIndex = 0;
  }
}
