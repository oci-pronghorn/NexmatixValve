package com.ociweb;

import com.ociweb.iot.maker.FogRuntime;

public class FogLight {
	public static void main(String[] args) {
		FogRuntime.run(new NexmatixValve());
	}
}
