/*
 * aTalk, android VoIP and Instant Messaging client
 * Copyright 2014 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.crypto.omemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import org.atalk.android.R;
import org.atalk.android.aTalkApp;
import org.atalk.android.gui.chat.ChatFragment;
import org.atalk.android.gui.util.ViewUtil;
import org.atalk.crypto.CryptoFragment;
import org.atalk.service.osgi.OSGiActivity;
import org.atalk.util.CryptoHelper;
import org.atalk.util.Logger;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.TrustState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.atalk.android.R.id.fingerprint;

/**
 * OMEMO buddy authenticate dialog.
 *
 * @author Eng Chong Meng
 */
public class OmemoAuthenticateDialog extends OSGiActivity
{
    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(OmemoAuthenticateDialog.class);

    private static OmemoManager mOmemoManager;
    private static HashSet<OmemoDevice> mOmemoDevices;
    private static CryptoFragment mCryptoFragment;
    private SQLiteOmemoStore mOmemoStore;

    private final HashMap<OmemoDevice, String> buddyFingerprints = new HashMap<>();
    private final LinkedHashMap<OmemoDevice, FingerprintStatus> deviceFPStatus = new LinkedHashMap<>();
    private final HashMap<OmemoDevice, Boolean> fingerprintCheck = new HashMap<>();

    /**
     * Fingerprints adapter instance.
     */
    private FingerprintListAdapter fpListAdapter;

    private OmemoFingerprint remoteFingerprint = null;
    private String bareJid;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mOmemoStore = (SQLiteOmemoStore) SignalOmemoService.getInstance().getOmemoStoreBackend();

        setContentView(R.layout.omemo_authenticate_dialog);
        setTitle(R.string.omemo_authbuddydialog_AUTHENTICATE_BUDDY);

        fpListAdapter = new FingerprintListAdapter(getBuddyFingerPrints());
        ListView fingerprintsList = findViewById(R.id.fp_list);
        fingerprintsList.setAdapter(fpListAdapter);

        String account = mOmemoManager.getOwnJid().toString();
        String localFingerprint = null;
        try {
            localFingerprint = mOmemoManager.getOwnFingerprint().toString();
        } catch (SmackException.NotLoggedInException e) {
            e.printStackTrace();
        } catch (CorruptedOmemoKeyException e) {
            e.printStackTrace();
        }

        View content = findViewById(android.R.id.content);
        ViewUtil.setTextViewValue(content, R.id.localFingerprintLbl,
                getString(R.string.omemo_authbuddydialog_LOCAL_FINGERPRINT, account,
                        CryptoHelper.prettifyFingerprint(localFingerprint)));
    }

    /**
     * Gets the list of all known buddyFPs.
     *
     * @return the list of all known buddyFPs.
     */
    Map<OmemoDevice, String> getBuddyFingerPrints()
    {
        String fingerprint;
        FingerprintStatus fpStatus;

        for (OmemoDevice device : mOmemoDevices) {
            try {
                fingerprint = mOmemoManager.getFingerprint(device).toString();
                buddyFingerprints.put(device, fingerprint);

                fpStatus = mOmemoStore.getFingerprintStatus(device, fingerprint);
                deviceFPStatus.put(device, fpStatus);
            } catch (CannotEstablishOmemoSessionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (SmackException.NotLoggedInException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (CorruptedOmemoKeyException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
        return buddyFingerprints;
    }

    /**
     * Method fired when the ok button is clicked.
     *
     * @param v ok button's <tt>View</tt>.
     */
    public void onOkClicked(View v)
    {
        boolean allTrusted = true;
        String fingerprint;

        for (Map.Entry<OmemoDevice, Boolean> entry : fingerprintCheck.entrySet()) {
            OmemoDevice omemoDevice = entry.getKey();
            Boolean fpCheck = entry.getValue();
            allTrusted = fpCheck && allTrusted;
            if (fpCheck) {
                fingerprint = buddyFingerprints.get(omemoDevice);
                trustOmemoFingerPrint(omemoDevice, fingerprint);
            }
            else {
                /* Do not change original fingerprint trust state */
                logger.warn("Leaving the fingerprintStatus as it: " + omemoDevice);
            }
        }

        int chatType;
        if (allTrusted) {
            chatType = ChatFragment.MSGTYPE_OMEMO;
        }
        else {
            chatType = ChatFragment.MSGTYPE_OMEMO_UA;
        }
        executeDone(chatType);
    }

    /**
     * Method fired when the cancel button is clicked.
     *
     * @param v the cancel button's <tt>View</tt>
     */
    public void onCancelClicked(View v)
    {
        executeDone(ChatFragment.MSGTYPE_OMEMO_UA);
    }

    private void executeDone(int chatType)
    {
        if (mCryptoFragment != null)
            mCryptoFragment.updateStatusOmemo(chatType);
        finish();
    }

    /**
     * Creates parametrized <tt>Intent</tt> of buddy authenticate dialog.
     *
     * @param omemoManager the UUID of OTR session.
     * @return buddy authenticate dialog parametrized with given OTR session's UUID.
     */
    public static Intent createIntent(OmemoManager omemoManager, HashSet<OmemoDevice> omemoDevices,
            CryptoFragment fragment)
    {
        Intent intent = new Intent(aTalkApp.getGlobalContext(), OmemoAuthenticateDialog.class);

        mOmemoManager = omemoManager;
        mOmemoDevices = omemoDevices;
        mCryptoFragment = fragment;

        // Started not from Activity
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    // ============== OMEMO Buddy FingerPrints Handlers ================== //
    private boolean isOmemoFPVerified(OmemoDevice omemoDevice, String fingerprint)
    {
        FingerprintStatus fpStatus = mOmemoStore.getFingerprintStatus(omemoDevice, fingerprint);
        return ((fpStatus != null) && fpStatus.isTrusted());
    }

    /**
     * Trust an OmemoIdentity. This involves marking the key as trusted.
     *
     * @param omemoDevice OmemoDevice
     * @param remoteFingerprint fingerprint
     */
    private void trustOmemoFingerPrint(OmemoDevice omemoDevice, String remoteFingerprint)
    {
        OmemoFingerprint omemoFingerprint = new OmemoFingerprint(remoteFingerprint);
        mOmemoStore.getTrustCallBack().setTrust(omemoDevice, omemoFingerprint, TrustState.trusted);
    }

    /**
     * Adapter displays fingerprints for given list of <tt>Contact</tt>s.
     */
    private class FingerprintListAdapter extends BaseAdapter
    {
        /**
         * The list of currently displayed buddy FingerPrints.
         */
        private final Map<OmemoDevice, String> buddyFPs;

        /**
         * Creates new instance of <tt>FingerprintListAdapter</tt>.
         *
         * @param linkedHashMap list of <tt>Contact</tt> for which OTR fingerprints will be displayed.
         */
        FingerprintListAdapter(Map<OmemoDevice, String> linkedHashMap)
        {
            buddyFPs = linkedHashMap;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getCount()
        {
            return buddyFPs.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getItem(int position)
        {
            return getOmemoDeviceFromRow(position);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getItemId(int position)
        {
            return position;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public View getView(int position, View rowView, ViewGroup parent)
        {
            if (rowView == null)
                rowView = getLayoutInflater().inflate(R.layout.omemo_fingerprint_row, parent, false);

            final OmemoDevice device = getOmemoDeviceFromRow(position);
            String remoteFingerprint = getFingerprintFromRow(position);

            ViewUtil.setTextViewValue(rowView, R.id.protocolProvider, device.toString());
            ViewUtil.setTextViewValue(rowView, fingerprint, CryptoHelper.prettifyFingerprint(remoteFingerprint));

            boolean isVerified = isOmemoFPVerified(device, remoteFingerprint);
            final CheckBox cb_fingerprint = (CheckBox) rowView.findViewById(R.id.fingerprint);
            cb_fingerprint.setChecked(isVerified);

            cb_fingerprint.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    fingerprintCheck.put(device, cb_fingerprint.isChecked());
                }
            });
            return rowView;
        }

        OmemoDevice getOmemoDeviceFromRow(int row)
        {
            int index = -1;
            for (OmemoDevice device : buddyFingerprints.keySet()) {
                index++;
                if (index == row) {
                    return device;
                }
            }
            return null;
        }

        String getFingerprintFromRow(int row)
        {
            int index = -1;
            for (String fingerprint : buddyFingerprints.values()) {
                index++;
                if (index == row) {
                    return fingerprint;
                }
            }
            return null;
        }
    }
}
