package com.qualcomm.qti.networksetting;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.telephony.OperatorInfo;
import java.util.List;

public interface INetworkQueryServiceCallback extends IInterface {

    public static abstract class Stub extends Binder implements INetworkQueryServiceCallback {
        private static final String DESCRIPTOR = "com.qualcomm.qti.networksetting.INetworkQueryServiceCallback";
        static final int TRANSACTION_onIncrementalManualScanResult = 2;
        static final int TRANSACTION_onQueryComplete = 1;

        private static class Proxy implements INetworkQueryServiceCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void onQueryComplete(List<OperatorInfo> networkInfoArray, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(networkInfoArray);
                    _data.writeInt(status);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onIncrementalManualScanResult(String[] scaninfo, int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStringArray(scaninfo);
                    _data.writeInt(status);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkQueryServiceCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetworkQueryServiceCallback)) {
                return new Proxy(obj);
            }
            return (INetworkQueryServiceCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        onQueryComplete(data.createTypedArrayList(OperatorInfo.CREATOR), data.readInt());
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        onIncrementalManualScanResult(data.createStringArray(), data.readInt());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onIncrementalManualScanResult(String[] strArr, int i) throws RemoteException;

    void onQueryComplete(List<OperatorInfo> list, int i) throws RemoteException;
}
