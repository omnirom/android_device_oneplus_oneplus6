package com.qualcomm.qti.networksetting;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INetworkQueryService extends IInterface {

    public static abstract class Stub extends Binder implements INetworkQueryService {
        private static final String DESCRIPTOR = "com.qualcomm.qti.networksetting.INetworkQueryService";
        static final int TRANSACTION_startNetworkQuery = 1;
        static final int TRANSACTION_stopNetworkQuery = 2;
        static final int TRANSACTION_unregisterCallback = 3;

        private static class Proxy implements INetworkQueryService {
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

            public void startNetworkQuery(INetworkQueryServiceCallback cb, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeInt(subId);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void stopNetworkQuery(INetworkQueryServiceCallback cb, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeInt(subId);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unregisterCallback(INetworkQueryServiceCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INetworkQueryService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INetworkQueryService)) {
                return new Proxy(obj);
            }
            return (INetworkQueryService) iin;
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
                        startNetworkQuery(com.qualcomm.qti.networksetting.INetworkQueryServiceCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        stopNetworkQuery(com.qualcomm.qti.networksetting.INetworkQueryServiceCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        unregisterCallback(com.qualcomm.qti.networksetting.INetworkQueryServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void startNetworkQuery(INetworkQueryServiceCallback iNetworkQueryServiceCallback, int i) throws RemoteException;

    void stopNetworkQuery(INetworkQueryServiceCallback iNetworkQueryServiceCallback, int i) throws RemoteException;

    void unregisterCallback(INetworkQueryServiceCallback iNetworkQueryServiceCallback) throws RemoteException;
}
