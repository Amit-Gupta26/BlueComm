BlueComm
=====================
A bunch of class to help you to deal with bluetooth on android.

## Getting Started
I'm planning to put this project on Gradle when it be ready to be used.

## Feature

- BlueCommDevice is Serializable, you can use BlueComm to save it in file.
- Get RSSI of BlueCommDevice.
- Use BlueComm to get device and manage bluetooth state:

		BlueComm.getInstance().getPairedDevice();		// Bluetooth device paired
        BlueComm.getInstance().getReachableDevice();	// Bluetooth device paired and see by the device
    
- Use BlueCommDevice to connect device and manage device:

		BlueCommDevice.pairDevice();
		BlueCommDevice.connect();
		BlueCommDevice.send(String msg);        
    
- Enable bluetooth on device ( Don't forget to ask user before open bluetooth ! ):

		isBluetoothAvailable()
    	isBluetoothOpen()
    	openBluetooth()
    	closeBluetooth()
    
- Use custom BlueCommDevice Class, you just have to set it to BlueComm:
		
        BlueComm.getInstance().setDeviceClass(BlueDeviceTest.class);

## Disclam
- This project is still under development. It's not yet use in production and it's not yet ready for production.
- This project was originally developped to use with HC-05 and HC-06 module.

## License
	Copyright 2015 Samuel Côté

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.





