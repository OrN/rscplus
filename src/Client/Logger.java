/**
 *	rscplus
 *
 *	This file is part of rscplus.
 *
 *	rscplus is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	rscplus is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with rscplus.  If not, see <http://www.gnu.org/licenses/>.
 *
 *	Authors: see <https://github.com/OrN/rscplus>
 */

package Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.fusesource.jansi.Ansi;

/**
 * A simple logger
 */
public class Logger {
	
	private static final String[] m_logTypeName = { "ERROR", " WARN", " GAME", " INFO", " DEBUG" };
	private static PrintWriter m_logWriter;
	
	public enum Type {
		ERROR(0), WARN(1), GAME(2), INFO(3), DEBUG(4);
		
		Type(int id) {
			this.id = id;
		}
		
		public int id;
	}
	
	public static void start() {
		File file = new File(Settings.Dir.JAR + "/log.txt");
		try {
			m_logWriter = new PrintWriter(new FileOutputStream(file));
		} catch (Exception e) {
		}
	}
	
	public static void stop() {
		try {
			m_logWriter.close();
		} catch (Exception e) {
		}
	}
	
	public static void Log(Type type, String message, boolean appends_console) {
		if (type.id > Settings.LOG_VERBOSITY.get(Settings.currentProfile))
			return;
		
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		String msg = "[" + m_logTypeName[type.id] + "]" +
					 "[" + dateFormat.format(new Date()) + "]" +
					 " " + message;
		
		if (type != Type.ERROR) {
            if (!appends_console) {
                if (Settings.APPEND_LOGGER_CONSOLE_TEXT.get(Settings.currentProfile)) {
                    System.out.println(msg);
                } else {
                    System.out.println(message);
                }
            } else {
                System.out.println(msg);
            }
        } else {
			System.err.println(msg);
		}
        
		try {
			m_logWriter.write(msg + "\r\n");
			m_logWriter.flush();
		} catch (Exception e) {
		}
	}
	
	public static void Error(String message) {
		Log(Type.ERROR, message, true);
	}
	
	public static void Warn(String message) {
		Log(Type.WARN, message, true);
	}
	
	public static void Game(String message) {
		Log(Type.GAME, message, true);
	}
	
	public static void Info(String message) {
		Log(Type.INFO, message, true);
	}
	
	public static void Debug(String message) {
		Log(Type.DEBUG, message, true);
	}
	
	public static void Warn(Ansi message) {
		Log(Type.WARN, message.toString(), true);
	}
	
	public static void Error(Ansi message) {
		Log(Type.ERROR, message.toString(), true);
	}
	
	public static void Game(Ansi message) {
		Log(Type.GAME, message.toString(), false);
	}
	
	public static void Info(Ansi message) {
		Log(Type.INFO, message.toString(), true);
	}
	
	public static void Debug(Ansi message) {
		Log(Type.DEBUG, message.toString(), true);
	}
	
    public static void Chat(String message) {
        Log(Type.INFO, message, false);
    }
}
