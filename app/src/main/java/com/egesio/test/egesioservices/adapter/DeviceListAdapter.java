package com.egesio.test.egesioservices.adapter;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.egesio.test.egesioservices.R;
import com.egesio.test.egesioservices.bean.DeviceBean;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mLeDevices;
    private ArrayList<DeviceBean> deviceBeens;
    private Context context;
    private DeviceBean deviceBean;

    public DeviceListAdapter(Context context, ArrayList<DeviceBean> deviceBeens) {
        super();
        this.mLeDevices = new ArrayList<>();
        this.deviceBeens = deviceBeens;
        this.context = context;
    }

    public void addDevice(DeviceBean deviceBean) {
        if (!mLeDevices.contains(deviceBean.getDevice())) {
            deviceBeens.add(deviceBean);
        }
    }

    public void addDevice(BluetoothDevice deviceBean) {
        if (!mLeDevices.contains(deviceBean)) {
            mLeDevices.add(deviceBean);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return deviceBeens.get(position).getDevice();
    }

    public void clear() {
        deviceBeens.clear();
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return deviceBeens.size();
    }

    @Override
    public Object getItem(int i) {
        return deviceBeens.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        /*ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.rssi = (TextView) view.findViewById(R.id.rssi);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        BluetoothDevice device = deviceBeens.get(i).getDevice();
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.unknown_device);
        viewHolder.deviceAddress.setText(device.getAddress());
        viewHolder.rssi.setText(String.valueOf(deviceBeens.get(i).getRssi()));

        return view;*/
        return null;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView rssi;
    }
}
