package com.fqxd.gftools.vpn.tunnel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.fqxd.gftools.vpn.VPNConstants;
import com.fqxd.gftools.vpn.nat.NatSession;
import com.fqxd.gftools.vpn.nat.NatSessionManager;
import com.fqxd.gftools.vpn.processparse.PortHostService;
import com.fqxd.gftools.vpn.utils.ACache;
import com.fqxd.gftools.vpn.utils.TcpDataSaveHelper;
import com.fqxd.gftools.vpn.utils.ThreadProxy;
import com.fqxd.gftools.vpn.utils.TimeFormatUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

/**
 * Created by zengzheying on 15/12/31.
 */
public class RemoteTcpTunnel extends RawTcpTunnel {
    Context context;
    TcpDataSaveHelper helper;
    NatSession session;
    private final Handler handler;

    public RemoteTcpTunnel(InetSocketAddress serverAddress, Selector selector, short portKey,Context context) throws IOException {
        super(serverAddress, selector, portKey);
        this.context = context;
        session = NatSessionManager.getSession(portKey);
        String helperDir = new StringBuilder()
                .append(VPNConstants.DATA_DIR)
                .append(TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime))
                .append("/")
                .append(session.getUniqueName())
                .toString();

        helper = new TcpDataSaveHelper(helperDir,context);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        super.afterReceived(buffer);
        refreshSessionAfterRead(buffer.limit());
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .isRequest(false)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();
        helper.addData(saveData);

    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {
        super.beforeSend(buffer);
        TcpDataSaveHelper.SaveData saveData = new TcpDataSaveHelper
                .SaveData
                .Builder()
                .isRequest(true)
                .needParseData(buffer.array())
                .length(buffer.limit())
                .offSet(0)
                .build();
        helper.addData(saveData);
        refreshAppInfo();

    }

    private void refreshAppInfo() {
        if (session.appInfo != null) {
            return;
        }
        if (PortHostService.getInstance() != null) {
            ThreadProxy.getInstance().execute(new Runnable() {
                @Override
                public void run() {
                    PortHostService.getInstance().refreshSessionInfo();
                }
            });
        }
    }

    private void refreshSessionAfterRead(int size) {

        session.receivePacketNum++;
        session.receiveByteNum += size;

    }

    @Override
    protected void onDispose() {
        super.onDispose();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ThreadProxy.getInstance().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (session.receiveByteNum == 0 && session.bytesSent == 0) {
                            return;
                        }

                        String configFileDir = VPNConstants.CONFIG_DIR
                                +TimeFormatUtil.formatYYMMDDHHMMSS(session.vpnStartTime) ;
                        File parentFile = new File(configFileDir);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        //说已经存了
                        File file = new File(parentFile, session.getUniqueName());
                        if (file.exists()) {
                            return;
                        }
                        ACache configACache = ACache.get(parentFile);
                        configACache.put(session.getUniqueName(), session);
                    }
                });
            }
        }, 1000);
    }
}
