/*
 * SPDX-FileCopyrightText:  2025 Peter Hasse <peter.hasse@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Johann Hackler <johann.hackler@fokus.fraunhofer.de>
 * SPDX-FileCopyrightText: 2025 Fraunhofer FOKUS
 *
 *  SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package de.fraunhofer.fokus.OpenMobileNetworkToolkit.DataProvider.CellInformations;

import android.os.Build;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthWcdma;
import android.util.Log;

import androidx.annotation.NonNull;

import com.influxdb.client.write.Point;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WCDMAInformation extends CellInformation {

    private static final Pattern REGEX_RSSI = Pattern.compile("ss=([^ ]*)");
    private static final Pattern REGEX_RSCP = Pattern.compile("rscp=([^ ]*)");
    private static final Pattern REGEX_BER = Pattern.compile("ber=([^ ]*)");
    private static final Pattern REGEX_ECNO = Pattern.compile("ecno=([^ ]*)");

    private static final int MIN_RSSI = -113;
    private static final int MIN_RSCP = -120;
    private static final int MAX_RSCP = -24;
    private static final int MIN_BER = 0;
    private static final int MIN_ECNO = -24;

    private int rssi;
    private int rscp;
    private int ber;
    private int EcNo;
    private int AsuLevel;

    public WCDMAInformation(long timestamp, CellSignalStrengthWcdma cellSignalStrengthWcdma) {
        super(timestamp);
        mapSignals(cellSignalStrengthWcdma);
        this.setCellType(CellType.WCDMA);

    }

    private WCDMAInformation(CellInfoWcdma cellInfoWcdma, CellIdentityWcdma cellIdentityWcdma, CellSignalStrengthWcdma cellSignalStrengthWcdma, long timestamp) {
        super(
                timestamp,
                CellType.WCDMA,
                String.valueOf(cellIdentityWcdma.getUarfcn()),
                cellIdentityWcdma.getCid(),
                cellIdentityWcdma.getMccString(),
                cellIdentityWcdma.getMncString(),
                cellIdentityWcdma.getPsc(),
                cellIdentityWcdma.getLac(),
                cellSignalStrengthWcdma.getLevel(),
                Objects.requireNonNull(cellIdentityWcdma.getOperatorAlphaLong()).toString(),
                cellSignalStrengthWcdma.getAsuLevel(),
                cellInfoWcdma.isRegistered(),
                cellInfoWcdma.getCellConnectionStatus()
        );
        mapSignals(cellSignalStrengthWcdma);

    }

    private void mapSignals(CellSignalStrengthWcdma cellSignalStrengthWcdma) {
        // For WCDMA not the correct API from the CellSignalStrengthWcdma is provided, hence we extract from the string object
        rssi = (extractSignal(cellSignalStrengthWcdma.toString(), REGEX_RSSI) == null) ? MIN_RSSI : extractSignal(cellSignalStrengthWcdma.toString(), REGEX_RSSI);
        rscp = (extractSignal(cellSignalStrengthWcdma.toString(), REGEX_RSCP) == null) ? MIN_RSCP : extractSignal(cellSignalStrengthWcdma.toString(), REGEX_RSCP);
        /* on samsung devices, when rscp is max value we have to use rssi as rscp value (bug) */
        if(Build.MANUFACTURER.equalsIgnoreCase("samsung") && rscp == -24) {
            rscp = rssi;
        }
        ber = (extractSignal(cellSignalStrengthWcdma.toString(), REGEX_BER) == null) ? MIN_BER : extractSignal(cellSignalStrengthWcdma.toString(), REGEX_BER);
        EcNo = (extractSignal(cellSignalStrengthWcdma.toString(), REGEX_ECNO) == null) ? MIN_ECNO : extractSignal(cellSignalStrengthWcdma.toString(), REGEX_ECNO);
    }

    public WCDMAInformation(CellInfoWcdma cellInfoWcdma, long timestamp) {
        this(cellInfoWcdma, cellInfoWcdma.getCellIdentity(), cellInfoWcdma.getCellSignalStrength(), timestamp);
    }

    public int getRssi() {
        return rssi;
    }

    public int getRscp() {
        return rscp;
    }

    public int getEcNo() {
        return EcNo;
    }

    public int getBer() {
        return ber;
    }

    public String getRssiString() {
        return Integer.toString(rssi);
    }

    public String getRscpString() {
        return Integer.toString(rscp);
    }

    public String getEcnoString() {
        return Integer.toString(EcNo);
    }

    public String getBerString() {
        return Integer.toString(ber);
    }

    public String getAsuLevelString() {
        return Integer.toString(AsuLevel);
    }

    public Integer extractSignal(String inputString, @NonNull Pattern regex) {
        Matcher matcher = regex.matcher(inputString);
        if (matcher.find()) {
            String valueStr = matcher.group(1);
            try {
                if (valueStr == null) {
                    return null;
                }
                return Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                Log.e("WCDMAInformation", "extracting signal strength WCDMA failed for: " + valueStr);
                return null;
            }
        }
        return null;
    }

    @Override
    public Point getPoint(Point point) {
        super.getPoint(point);
        point.addField("RSSI", rssi);
        point.addField("RSCP", rscp);
        point.addField("BER", ber);
        point.addField("EcNo", EcNo);
        return point;
    }


}
