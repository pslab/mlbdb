/*
 * Created on May 29, 2006 2:13:41 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package com.aelitis.azureus.ui.swt;

import java.util.Iterator;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.mainwindow.*;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.PreUpdateChecker;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.messenger.config.PlatformConfigMessenger;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.swt.shells.main.MainWindow;
import com.aelitis.azureus.ui.swt.utils.UIUpdaterFactory;
import com.aelitis.azureus.util.Constants;
import com.aelitis.azureus.util.InitialisationFunctions;

/**
 * @author TuxPaper
 * @created May 29, 2006
 *
 */
public class Initializer
	implements IUIIntializer
{
	private static StartServer startServer;

	private final AzureusCore core;

	protected static SplashWindow splash;

	private final String[] args;

	private CopyOnWriteList listeners = new CopyOnWriteList();

	private AEMonitor listeners_mon = new AEMonitor("Initializer:l");

	private int curPercent = 0;

	public static void main(final String args[]) {
		if (System.getProperty("ui.temp") == null) {
			System.setProperty("ui.temp", "az3");
		}

		Main.main(args);
	}

	/**
	 * Main Initializer
	 * @param core
	 * @param args
	 */
	public Initializer(AzureusCore core, boolean createSWTThreadAndRun,
			String[] args) {
		this.core = core;
		this.args = args;

		if (createSWTThreadAndRun) {
			try {
				SWTThread.createInstance(this);
			} catch (SWTThreadAlreadyInstanciatedException e) {
				Debug.printStackTrace(e);
			}
		} else {
			Constants.initialize(core);
			PlatformConfigMessenger.login(0);
		}
	}

	public void run() {
		// initialise the SWT locale util
		long startTime = SystemTime.getCurrentTime();

		new LocaleUtilSWT(core);

		final Display display = SWTThread.getInstance().getDisplay();

		UIConfigDefaultsSWT.initialize();

		UIConfigDefaultsSWTv3.initialize(core);

		ImageRepository.loadImagesForSplashWindow(display);

		ImageRepository.addPath("com/aelitis/azureus/ui/images/azureus.jpg",
				"azureus_splash");

		if (COConfigurationManager.getBooleanParameter("Show Splash")) {
			display.syncExec(new AERunnable() {
				public void runSupport() {
					splash = new SplashWindow(display, Initializer.this);
				}
			});
		}

		System.out.println("Locale Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		core.addListener(new AzureusCoreListener() {
			int fakePercent = 80;

			long startTime = SystemTime.getCurrentTime();

			String sLastTask;

			public void reportCurrentTask(AzureusCoreOperation op, String currentTask) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}

				Initializer.this.reportCurrentTask(currentTask);
				if (fakePercent > 0) {
					fakePercent--;
					Initializer.this.reportPercent(curPercent + 1);
				}

				if (sLastTask != null && !sLastTask.startsWith("Loading Torrent")) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
					startTime = SystemTime.getCurrentTime();
				}
				sLastTask = currentTask;
				//System.out.println(currentTask);
			}

			public void reportPercent(AzureusCoreOperation op, int percent) {
				if (op.getOperationType() != AzureusCoreOperation.OP_INITIALISATION) {
					return;
				}
				if (percent == 100) {
					long now = SystemTime.getCurrentTime();
					long diff = now - startTime;
					if (diff > 10 && diff < 1000 * 60 * 5) {
						System.out.println("   Core: " + diff + "ms for " + sLastTask);
					}
				}
				// TODO Auto-generated method stub
			}

		});

		core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
			private GlobalManager gm;

			public void componentCreated(AzureusCore core,
					AzureusCoreComponent component) {
				Initializer.this.reportPercent(curPercent + 1);
				if (component instanceof GlobalManager) {
					reportCurrentTaskByKey("splash.initializePlugins");

					gm = (GlobalManager) component;

					InitialisationFunctions.earlyInitialisation(core);
				}
			}

			// @see com.aelitis.azureus.core.AzureusCoreLifecycleAdapter#started(com.aelitis.azureus.core.AzureusCore)
			public void started(AzureusCore core) {
				InitialisationFunctions.lateInitialisation(core);
				if (gm == null) {
					return;
				}

				// Ensure colors initialized
				Colors.getInstance();
				
				Initializer.this.reportPercent(curPercent + 1);
				new UserAlerts(gm);

				reportCurrentTaskByKey("splash.initializeGui");

				Initializer.this.reportPercent(curPercent + 1);
				Cursors.init();

				Initializer.this.reportPercent(curPercent + 1);
				new MainWindow(core, Display.getDefault(), splash);

				reportCurrentTaskByKey("splash.openViews");

				SWTUpdateChecker.initialize();

				PreUpdateChecker.initialize(core,COConfigurationManager.getStringParameter("ui"));

				UpdateMonitor.getSingleton(core); // setup the update monitor
				
				//Tell listeners that all is initialized :
				Alerts.initComplete();

				//Finally, open torrents if any.
				for (int i = 0; i < args.length; i++) {

					try {
						TorrentOpener.openTorrent(args[i]);

					} catch (Throwable e) {

						Debug.printStackTrace(e);
					}
				}
			}

			public void stopping(AzureusCore core) {
				Alerts.stopInitiated();
			}

			public void stopped(AzureusCore core) {
			}

			public boolean syncInvokeRequired() {
				return (true);
			}

			public boolean stopRequested(AzureusCore _core)
					throws AzureusCoreException {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(false);
			}

			public boolean restartRequested(final AzureusCore core) {
				return org.gudy.azureus2.ui.swt.mainwindow.Initializer.handleStopRestart(true);
			}

		});

		reportCurrentTaskByKey("splash.initializeCore");

		core.start();

		reportPercent(80);

		System.out.println("Core Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
		startTime = SystemTime.getCurrentTime();

		reportCurrentTaskByKey("splash.initializeUIElements");

		// Ensure colors initialized
		Colors.getInstance();

		reportPercent(curPercent + 1);
		Alerts.init();

		reportPercent(curPercent + 1);
		ProgressWindow.register(core);

		reportPercent(curPercent + 1);
		new SWTNetworkSelection();

		reportPercent(curPercent + 1);
		new AuthenticatorWindow();

		reportPercent(curPercent + 1);
		new CertificateTrustWindow();

		reportPercent(90);

		InstallPluginWizard.register(core, display);

		System.out.println("GUI Initializing took "
				+ (SystemTime.getCurrentTime() - startTime) + "ms");
	}


	public void stopIt(boolean isForRestart, boolean isCloseAreadyInProgress)
			throws AzureusCoreException {
		if (core != null && !isCloseAreadyInProgress) {

			if (isForRestart) {

				core.checkRestartSupported();
			}
		}

		try {
			if (startServer != null) {
				startServer.stopIt();
			}

			//			Cursors.dispose();

			UIUpdaterFactory.getInstance().stopIt();

			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					SWTThread.getInstance().terminate();
				}
			});

		} finally {

			if (core != null && !isCloseAreadyInProgress) {

				try {
					if (isForRestart) {

						core.restart();

					} else {

						long lStopStarted = System.currentTimeMillis();
						System.out.println("core.stop");
						core.stop();
						System.out.println("core.stop done in "
								+ (System.currentTimeMillis() - lStopStarted));
					}
				} catch (Throwable e) {

					// don't let any failure here cause the stop operation to fail

					Debug.out(e);
				}
			}
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#addListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void addListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.add(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	// @see com.aelitis.azureus.ui.IUIIntializer#removeListener(org.gudy.azureus2.ui.swt.mainwindow.InitializerListener)
	public void removeListener(InitializerListener listener) {
		try {
			listeners_mon.enter();

			listeners.remove(listener);
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTask(String currentTaskString) {
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportCurrentTask(currentTaskString);
				} catch (Exception e) {
					// ignore
				}
			}
		} finally {

			listeners_mon.exit();
		}
	}

	private void reportCurrentTaskByKey(String key) {
		reportCurrentTask(MessageText.getString(key));
	}

	public void reportPercent(int percent) {
		if (curPercent > percent) {
			return;
		}

		curPercent = percent;
		try {
			listeners_mon.enter();

			Iterator iter = listeners.iterator();
			while (iter.hasNext()) {
				InitializerListener listener = (InitializerListener) iter.next();
				try {
					listener.reportPercent(percent);
				} catch (Exception e) {
					// ignore
				}
			}

			if (percent > 100) {
				listeners.clear();
			}
		} finally {

			listeners_mon.exit();
		}
	}
}