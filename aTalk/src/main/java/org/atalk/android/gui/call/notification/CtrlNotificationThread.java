/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.atalk.android.gui.call.notification;

import java.util.Iterator;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.GuiUtils;
import net.java.sip.communicator.util.Logger;

import org.atalk.android.gui.call.CallManager;
import org.atalk.android.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 * Class runs the thread that updates call control notification.
 *
 * @author Pawel Domas
 */
class CtrlNotificationThread
{
	/**
	 * The logger.
	 */
	private static final Logger logger = Logger.getLogger(CtrlNotificationThread.class);
	/**
	 * Notification update interval.
	 */
	private static final long UPDATE_INTERVAL = 1000;
	/**
	 * The thread that does the updates.
	 */
	private Thread thread;
	/**
	 * Flag used to stop the thread.
	 */
	private boolean run = true;
	/**
	 * The call control notification that is being updated by this thread.
	 */
	private final Notification notification;
	/**
	 * The Android context.
	 */
	private final Context ctx;
	/**
	 * The notification ID.
	 */
	final int id;
	/**
	 * The call that is controlled by notification.
	 */
	private final Call call;

	/**
	 * Creates new instance of {@link CtrlNotificationThread}.
	 * 
	 * @param ctx
	 *        the Android context.
	 * @param call
	 *        the call that is controlled by current notification.
	 * @param id
	 *        the notification ID.
	 * @param notification
	 *        call control notification that will be updated by this thread.
	 */
	public CtrlNotificationThread(Context ctx, Call call, int id, Notification notification) {
		this.ctx = ctx;
		this.id = id;
		this.notification = notification;
		this.call = call;
	}

	/**
	 * Starts notification update thread.
	 */
	public void start()
	{
		this.thread = new Thread(new Runnable() {
			public void run()
			{
				notificationLoop();
			}
		});
		thread.start();
	}

	private void notificationLoop()
	{
		while (run) {
			logger.trace("Running control notification thread " + hashCode());

			// Call duration timer
			long callStartDate = CallPeer.CALL_DURATION_START_TIME_UNKNOWN;
			Iterator<? extends CallPeer> peers = call.getCallPeers();
			if (peers.hasNext()) {
				callStartDate = peers.next().getCallDurationStartTime();
			}
			if (callStartDate != CallPeer.CALL_DURATION_START_TIME_UNKNOWN) {
				notification.contentView.setTextViewText(R.id.call_duration, GuiUtils.formatTime(callStartDate, System.currentTimeMillis()));
			}

			boolean isMute = CallManager.isMute(call);
			notification.contentView.setInt(R.id.mute_button, "setBackgroundResource", isMute ? R.drawable.status_btn_on : R.drawable.status_btn_off);

			boolean isOnHold = CallManager.isLocallyOnHold(call);
			notification.contentView.setInt(R.id.hold_button, "setBackgroundResource", isOnHold ? R.drawable.status_btn_on : R.drawable.status_btn_off);

			NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
			if (run) {
				mNotificationManager.notify(id, notification);
			}

			synchronized (this) {
				try {
					this.wait(UPDATE_INTERVAL);
				}
				catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	/**
	 * Stops notification thread.
	 */
	public void stop()
	{
		run = false;

		synchronized (this) {
			this.notifyAll();
		}

		try {
			thread.join();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
